(function () {
  var api = '/api/v1';
  var input = document.getElementById('q');
  var result = document.getElementById('result');
  var usage = document.getElementById('usage');
  var message = document.getElementById('message');
  function text(value) { message.textContent = value || ''; }
  function mask(number) { return '•••• •••• •••• ' + number.slice(-4); }
  function request(url, options) {
    return fetch(url, options).then(function (response) {
      if (response.ok) return response.json();
      return response.json().catch(function () { return {}; }).then(function (error) { throw new Error(error.detail || 'Request failed'); });
    });
  }
  function search() {
    text('Searching…');
    request(api + '/customers?query=' + encodeURIComponent(input.value)).then(function (rows) {
      if (!rows.length) { result.innerHTML = '<p>No matching customer.</p>'; text(''); return; }
      var html = '<ul class="list">';
      rows.forEach(function (customer) { html += '<li><a href="/customer.html?id=' + customer.customer_id + '"><strong>' + customer.first_name + ' ' + customer.last_name + '</strong><br><span class="muted">Customer ' + customer.customer_id + ' · ' + (customer.email || '') + '</span></a></li>'; });
      result.innerHTML = html + '</ul>'; text('');
    }).catch(function (error) { text(error.message); });
  }
  document.getElementById('search-button').addEventListener('click', search);
  input.addEventListener('keydown', function (event) { if (event.key === 'Enter') search(); });
  request(api + '/usage').then(function (metrics) { usage.innerHTML = metrics.map(function (metric) { return '<div>' + metric.event + ': <strong>' + metric.count.toLocaleString() + '</strong></div>'; }).join(''); }).catch(function () {});
  search();
}());
