
docker-compose -f .\docker-compose-v8.yaml up -d

docker exec -it elasticsearch-apm-v8 bash

elasticsearch@elasticsearch:~$ ./bin/elasticsearch-create-enrollment-token --scope kibana
WARNING: Owner of file [/usr/share/elasticsearch/config/users] used to be [root], but now is [elasticsearch]
WARNING: Owner of file [/usr/share/elasticsearch/config/users_roles] used to be [root], but now is [elasticsearch]
eyJ2ZXIiOiI4LjExLjEiLCJhZHIiOlsiMTcyLjIzLjAuMjo5MjAwIl0sImZnciI6IjU0ZTc5MDZlZDMxMzNkZGI5ZDEwYzVlNjExZTJlYzdjMTI2MjZlMGM1ODM5YTE1OTRjMGM1NzQwY2JiZjY5YmEiLCJrZXkiOiJxbm1xUTR3QnBBM2p0SzY0UEJVNzo0V0tvWU5hYlFkbXdzOG5DT3JOUmFBIn0=


PS C:\Users\gonin\Documents\projects\7_elastic\blog-examples> docker exec -it kibana-apm-v8 bash       
kibana@76909aaac091:~$ ./bin/kibana-verification-code    
Kibana is currently running with legacy OpenSSL providers enabled! For details and instructions on how to disable see https://www.elastic.co/guide/en/kibana/8.11/production.html#openssl-legacy-provider
Your verification code is:  800 281 

bin/elasticsearch-setup-passwords auto

*****************************************************************************

Initiating the setup of passwords for reserved users elastic,apm_system,kibana,kibana_system,logstash_system,beats_system,remote_monitoring_user.
The passwords will be randomly generated and printed to the console.
Please confirm that you would like to continue [y/N]y


Changed password for user apm_system
PASSWORD apm_system = q9oDOBXbgzWY2GZPcGRt

Changed password for user kibana_system
PASSWORD kibana_system = MHbZJBODoXeIxHZ2EcuD

Changed password for user kibana
PASSWORD kibana = MHbZJBODoXeIxHZ2EcuD

Changed password for user logstash_system
PASSWORD logstash_system = yvtzfEnKL5R0KG7OBA0Y

Changed password for user beats_system
PASSWORD beats_system = pDpTKxacK7pOH1moxCcq

Changed password for user remote_monitoring_user
PASSWORD remote_monitoring_user = LsZ5hB5EikNoGhU7b9oD

Changed password for user elastic
PASSWORD elastic = 5w6od0efCR3FEAZIZc9G
