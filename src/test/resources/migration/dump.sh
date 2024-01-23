#!/bin/sh

host="127.0.0.1"
port="5432"
dbname="hsh02_hsdb"
username="hsh02_hsdb_readonly"

target="/tmp"

dump() {
    sql="copy ($1) to stdout with csv header delimiter ';' quote '\"'"
    file="${target}/${2}"
    psql --host ${host} --port ${port} --user ${username} --command "${sql}" ${dbname} >"${file}"
}

dump "select bp_id, member_id, member_code, member_since, member_until, member_role, author_contract, nondisc_contract, free, exempt_vat, indicator_vat, uid_vat
      from business_partner
      order by bp_id" \
     "business-partners.csv" 
    
dump "select contact_id, bp_id, salut, first_name, last_name, title, firma, co, street, zipcode, city, country, phone_private, phone_office, phone_mobile, fax, email, array_to_string(array_agg(role), ',') as roles
      from contact
      left join contactrole_ref using(contact_id)
      group by contact_id
      order by contact_id" \
     "contacts.csv"

dump "select sepa_mandat_id, bp_id, bank_customer, bank_name, bank_iban, bank_bic, mandat_ref, mandat_signed, mandat_since, mandat_until, mandat_used
      from sepa_mandat
      order by sepa_mandat_id" \
     "sepa-mandates.csv"

dump "select member_asset_id, bp_id, date, action, amount, comment
     from member_asset
     WHERE bp_id NOT IN (511912)
     order by member_asset_id" \
     "asset-transactions.csv"

dump "select member_share_id, bp_id, date, action, quantity, comment
      from member_share
     WHERE bp_id NOT IN (511912)
      order by member_share_id" \
     "share-transactions.csv"
