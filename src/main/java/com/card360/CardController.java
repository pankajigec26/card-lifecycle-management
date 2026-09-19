package com.card360;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/v1")
public class CardController {
  private final JdbcTemplate db;
  public CardController(JdbcTemplate db) { this.db = db; }

  @GetMapping("/customers")
  public List<Map<String,Object>> customers(@RequestParam(required=false) String query) {
    String q = query == null ? "" : query.trim();
    return db.queryForList("select customer_id, first_name, last_name, email, phone, customer_status from customer where cast(customer_id as varchar) like ? or lower(first_name || ' ' || last_name) like lower(?) order by customer_id", "%"+q+"%", "%"+q+"%");
  }
  @GetMapping("/customers/{customerId}")
  public Map<String,Object> customer(@PathVariable long customerId) {
    Map<String,Object> customer = one("select * from customer where customer_id=?", customerId);
    customer.put("cards", db.queryForList("select c.card_id, c.card_number, c.card_type, c.card_status, c.expiry_date, c.credit_limit, c.available_limit, p.product_name from card c left join card_product p on p.product_id=c.product_id where c.customer_id=? order by c.card_id", customerId));
    return customer;
  }
  @GetMapping("/cards/{cardId}")
  public Map<String,Object> card(@PathVariable long cardId) {
    Map<String,Object> card = one("select c.*, p.product_name, cu.first_name, cu.last_name from card c join customer cu on cu.customer_id=c.customer_id left join card_product p on p.product_id=c.product_id where c.card_id=?", cardId);
    card.put("transactions", db.queryForList("select * from card_transaction where card_id=? order by transaction_date desc", cardId));
    return card;
  }
  @PatchMapping("/cards/{cardId}/activate") @Transactional
  public Map<String,Object> activate(@PathVariable long cardId) {
    Map<String,Object> card = one("select card_status, expiry_date from card where card_id=?", cardId);
    if (!"PENDING_ACTIVATION".equals(card.get("card_status"))) throw bad("Only a pending card can be activated.");
    if (((java.sql.Date)card.get("expiry_date")).toLocalDate().isBefore(LocalDate.now())) throw bad("An expired card cannot be activated.");
    db.update("update card set card_status='ACTIVE', activation_date=current_timestamp where card_id=?", cardId);
    return card(cardId);
  }
  @PatchMapping("/cards/{cardId}/block") @Transactional
  public Map<String,Object> block(@PathVariable long cardId, @RequestBody StatusRequest request) {
    Map<String,Object> card = one("select card_status from card where card_id=?", cardId);
    if (!"ACTIVE".equals(card.get("card_status"))) throw bad("Only an active card can be blocked.");
    if (request.reason == null || request.reason.trim().isEmpty()) throw bad("A block reason is required.");
    db.update("update card set card_status='BLOCKED' where card_id=?", cardId);
    db.update("insert into card_block(card_id,block_reason,blocked_by) values (?,?,?)",cardId,request.reason.toUpperCase(),"card360-user");
    return card(cardId);
  }
  @PatchMapping("/cards/{cardId}/unblock") @Transactional
  public Map<String,Object> unblock(@PathVariable long cardId) {
    Map<String,Object> card = one("select card_status from card where card_id=?", cardId);
    if (!"BLOCKED".equals(card.get("card_status"))) throw bad("Only a blocked card can be unblocked.");
    db.update("update card set card_status='ACTIVE' where card_id=?", cardId); return card(cardId);
  }
  @PostMapping("/cards/{cardId}/replacement") @Transactional
  public Map<String,Object> replace(@PathVariable long cardId, @RequestBody StatusRequest request) {
    Map<String,Object> old = one("select * from card where card_id=?", cardId);
    long newId = db.queryForObject("select coalesce(max(card_id),2000)+1 from card", Long.class);
    String oldNumber=(String)old.get("card_number"); String number=oldNumber.substring(0, 12)+String.format("%04d",newId%10000);
    db.update("insert into card(card_id,customer_id,product_id,card_number,card_type,card_status,issue_date,expiry_date,credit_limit,available_limit) values (?,?,?,? ,?,'PENDING_ACTIVATION',current_date,dateadd('YEAR',4,current_date),?,?)",newId,old.get("customer_id"),old.get("product_id"),number,old.get("card_type"),old.get("credit_limit"),old.get("available_limit"));
    db.update("update card set card_status='REPLACED' where card_id=?",cardId);
    db.update("insert into card_replacement(old_card_id,new_card_id,reason,replacement_status) values (?,?,?,'ISSUED')",cardId,newId,request.reason == null ? "CUSTOMER_REQUEST" : request.reason.toUpperCase());
    return card(newId);
  }
  @PatchMapping("/cards/{cardId}/limit") @Transactional
  public Map<String,Object> limit(@PathVariable long cardId, @RequestBody LimitRequest request) {
    if (request.creditLimit == null || request.creditLimit.compareTo(BigDecimal.ZERO) < 0) throw bad("Credit limit must be zero or greater.");
    Map<String,Object> old=one("select credit_limit, available_limit from card where card_id=?",cardId);
    BigDecimal spent=((BigDecimal)old.get("credit_limit")).subtract((BigDecimal)old.get("available_limit"));
    if (request.creditLimit.compareTo(spent)<0) throw bad("Credit limit cannot be below the outstanding balance.");
    db.update("update card set credit_limit=?, available_limit=? where card_id=?",request.creditLimit,request.creditLimit.subtract(spent),cardId);
    db.update("insert into card_limit_history(card_id,old_limit,new_limit,changed_by) values (?,?,?,'card360-user')",cardId,old.get("credit_limit"),request.creditLimit);
    return card(cardId);
  }
  @GetMapping("/usage") public List<Map<String,Object>> usage() {
    return Arrays.asList(metric("CARD_SEARCH",1250000),metric("VIEW_CARD",980000),metric("BLOCK_CARD",72000),metric("ACTIVATE_CARD",55000),metric("REPLACE_CARD",21000),metric("CHANGE_LIMIT",9000),metric("EXPORT_CARD_REPORT",17));
  }
  private Map<String,Object> metric(String event, int count) { Map<String,Object> m=new LinkedHashMap<>();m.put("event",event);m.put("count",count);return m; }
  private Map<String,Object> one(String sql,Object... args) { try { return db.queryForMap(sql,args); } catch (Exception e) { throw new ResponseStatusException(HttpStatus.NOT_FOUND,"Card or customer not found"); } }
  private ResponseStatusException bad(String message) { return new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,message); }
  public static class StatusRequest { public String reason; }
  public static class LimitRequest { public BigDecimal creditLimit; }
}
