#! /bin/sh

mysql() {
	docker rm -f mysql || true
	docker run --name mysql -e MYSQL_ROOT_PASSWORD=root -e MYSQL_DATABASE=test -p3306:3306 -d mysql:5.6.25 --character-set-server=utf8mb4 --collation-server=utf8mb4_unicode_ci
}

db2() {
    docker rm -f db2 || true
	docker run --name db2 -e DB2INST1_PASSWORD=db2inst1-pwd -e LICENSE=accept -p50000:50000 -d ibmcom/db2express-c:10.5.0.5-3.10.0 db2start
	docker exec -t db2 sudo -u db2inst1 /home/db2inst1/sqllib/bin/db2 create database test
}

if [ -z ${1} ]; then
	echo "No db name provided"
	echo "Provide one of:"
	echo -e "\tmysql"
	echo -e "\tdb2"
else
	${1}
fi