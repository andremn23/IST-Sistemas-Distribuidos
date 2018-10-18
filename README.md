# IST-Sistemas-Distribuidos
Projecto da cadeira Sistemas Distribuídos na linguagem Java

# Binas
Distributed Systems 2017-2018 project - Entrega final 
Grupo 67


## Instructions (verificar guia para mais detalhes)
1- Start a local jUDDI server ([instructions](http://disciplinas.tecnico.ulisboa.pt/leic-sod/2017-2018/labs/software/index.html))
(or set the properties for a remote jUDDI server)

Make sure that the uddi-naming library is installed.

2- Start a Station Web Service
```
cd station-ws
mvn clean compile exec:java
```
You can start additional servers by setting the _i_ and _coordinates_ properties: 
```
mvn exec:java -Dws.i=2 -Dstation.coords.x=80 -Dstation.coords.y=20
mvn exec:java -Dws.i=3 -Dstation.coords.x=50 -Dstation.coords.y=50

3 - Install ws-handlers 

mvn clean install
```

3- Install the Station Client in your local Maven Repository and run it
```
cd station-ws-cli
mvn clean install exec:java
```
The integration tests will run in the _verify_ phase, before the _install_ phase.

4- Start the Binas Web Service
```
cd binas-ws
mvn clean compile exec:java
```

5- Run Binas Client
```
cd binas-ws-cli
mvn clean compile exec:java
```
The client should call a test operation and print the result.
