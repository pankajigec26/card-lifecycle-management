merge into customer key(customer_id) values (1001,'Pankaj','Kumar','pankaj@example.com','+91 98765 43210','ACTIVE',current_timestamp);
merge into customer key(customer_id) values (1002,'John','Smith','john.smith@example.com','+1 555 0101','ACTIVE',current_timestamp);
merge into card_product key(product_id) values (1,'Platinum Credit','CREDIT',499.00,18.50);
merge into card_product key(product_id) values (2,'Everyday Debit','DEBIT',0.00,0.00);
merge into card key(card_id) values (2001,1001,1,'5412345678905412','CREDIT','ACTIVE',date '2024-01-15',date '2028-12-31',timestamp '2024-01-16 10:00:00',250000.00,238450.50);
merge into card key(card_id) values (2002,1001,2,'4218123456784218','DEBIT','BLOCKED',date '2023-08-01',date '2027-08-31',timestamp '2023-08-02 09:00:00',50000.00,50000.00);
merge into card key(card_id) values (2003,1002,1,'4111111111111111','CREDIT','PENDING_ACTIVATION',date '2026-09-01',date '2030-09-30',null,100000.00,100000.00);
merge into card_transaction key(transaction_id) values (3001,2001,timestamp '2026-09-15 12:15:00',1249.50,'INR','City Market','PURCHASE','SETTLED');
merge into card_transaction key(transaction_id) values (3002,2001,timestamp '2026-09-16 19:05:00',10300.00,'INR','Airline Express','PURCHASE','SETTLED');
