## run all services with docker-compose
```
docker-compose up -d
```
## open kibana with browser
```
http://localhost:5601
```
## generate enrollment token in elasticsearch
```
docker exec -it blog-examples-elasticsearch-1 sh
./bin/elasticsearch-create-enrollment-token --scope kibana

eyJ2ZXIiOiI4LjEwLjIiLCJhZHIiOlsiMTcyLjIwLjAuMjo5MjAwIl0sImZnciI6IjljNjNiMTVjZDM1ZjQ5YWFiZmFlYjMyNzA4NDBmNWNkZTNiMTAwOGE4NjNkNzMyMmMxMzJkNTk5NDU1ZWQxNzUiLCJrZXkiOiJNRnVueW9vQjF2OUhHUlpndmpYcTpyX2t5MXFYMlQ1V2dLbHZVRWpaWFNnIn0=
```
## copy code from kibana
```
docker exec -it blog-examples-kibana-1 sh
./bin/kibana-verification-code
```
