#!/bin/sh

host="127.0.0.1"
port="5432"
dbname="hsh02_hsdb"
username="hsh02_hsdb_readonly"

target="/tmp"
if [ ! -z $DEST ];
then
    target=$DEST
fi

dump() {
    sql="copy ($1) to stdout with csv header delimiter ';' quote '\"'"
    file="${target}/${2}"
    psql --host ${host} --port ${port} --user ${username} --command "${sql}" ${dbname} >"${file}"
}

dump "select bp_id, member_id, member_code, member_since, member_until, member_role, author_contract, nondisc_contract, free, exempt_vat, indicator_vat, uid_vat
      from business_partner
      order by bp_id" \
     "office/business_partners.csv"
    
dump "select contact_id, bp_id, salut, first_name, last_name, title, firma, co, street, zipcode, city, country, phone_private, phone_office, phone_mobile, fax, email, array_to_string(array_agg(role), ',') as roles
      from contact
      left join contactrole_ref using(contact_id)
      group by contact_id
      order by contact_id" \
     "office/contacts.csv"

dump "select sepa_mandat_id, bp_id, bank_customer, bank_name, bank_iban, bank_bic, mandat_ref, mandat_signed, mandat_since, mandat_until, mandat_used
      from sepa_mandat
      order by sepa_mandat_id" \
     "office/sepa_mandates.csv"

dump "select member_asset_id, bp_id, date, action, amount, comment
     from member_asset
     order by member_asset_id" \
     "office/asset_transactions.csv"

dump "select member_share_id, bp_id, date, action, quantity, comment
      from member_share
      order by member_share_id" \
     "office/share_transactions.csv"

dump "select inet_addr_id, inet_addr, description
      from inet_addr
      order by inet_addr_id" \
     "hosting/inet_addr.csv"

dump "select hive_id, hive_name, inet_addr_id, description
      from hive
      order by hive_id" \
     "hosting/hive.csv"

dump "select packet_id, basepacket_code, packet_name, bp_id, hive_id, created, cancelled, cur_inet_addr_id, old_inet_addr_id, free
      from packet
      left join basepacket using (basepacket_id)
      order by packet_id" \
     "hosting/packet.csv"

dump "select packet_component_id, packet_id, quantity, basecomponent_code, created, cancelled
      from packet_component
      left join basecomponent using (basecomponent_id)
      order by packet_component_id" \
     "hosting/packet_component.csv"

dump "select unixuser_id, name, comment, shell, homedir, locked, packet_id, userid, quota_softlimit, quota_hardlimit, storage_softlimit, storage_hardlimit
      from unixuser
      order by unixuser_id" \
     "hosting/unixuser.csv"

# weil das fehlt, muss group by komplett gesetzt werden: alter table domain add constraint PK_domain primary key (domain_id);
dump "select domain_id, domain_name, domain_since, domain_dns_master, domain_owner, valid_subdomain_names, passenger_python, passenger_nodejs, passenger_ruby, fcgi_php_bin, array_to_string(array_agg(domain_option_name), ',') as domainoptions
      from domain
      left join domain__domain_option using(domain_id)
      left join domain_option using (domain_option_id)
      group by domain.domain_id, domain.domain_name, domain_since, domain_dns_master, domain_owner, valid_subdomain_names, passenger_python, passenger_nodejs, passenger_ruby, fcgi_php_bin
      order by domain.domain_id" \
     "hosting/domain.csv"

dump "select emailaddr_id, domain_id, localpart, subdomain, target
      from emailaddr
      order by emailaddr_id" \
     "hosting/emailaddr.csv"

dump "select emailalias_id, pac_id, name, target
      from emailalias
      order by emailalias_id" \
     "hosting/emailalias.csv"

dump "select dbuser_id, engine, packet_id, name
      from database_user
      order by dbuser_id" \
     "hosting/database_user.csv"

dump "select database_id, engine, packet_id, name, owner, encoding
      from database
      order by database_id" \
     "hosting/database.csv"
