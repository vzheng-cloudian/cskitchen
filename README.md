# 1. Usage


## 1.1 Runnable Package

The runnable package is in the cli.zip . 

Run start.sh with -h option to print usage as below:

usage: -o < number > -ops < number > -q < number > -r < y|n > -t < 1|2|3 >

  -o < number >   --> Total number of orders, default is 100, range from 1 to 100,000.

  -ops < number > --> Order per second, default is 2, range from 1 to 100.

  -q < number >   --> Max queue length, default is 1000, range from 1 to 100,000.

  -r < y|n >      --> Randomly choosing food for orders, otherwise CheesePizza wil be chosen, default is [y]es.

  -t < 1|2|3 >    --> Match type, 1: MATCH, 2: FIFO, 3: both 1 & 2 , default is 3.
  
Run start.sh without option to invoke the system with total 100 orders and ops 2. The output will print to both the console and a logfile "cloudkitchen.log".


## 1.2 Source Code

Source code is in the code.zip . IntellJ IDEA is used for this project.


# 2. Design justification


## 2.1 
  
  Adopting multi-threading technology, to simulate multiple roles in a realtime system.
  

## 2.2 
  
  Applying abstraction, inheritance and polymorphism OOP concepts, make the system structure simple and extendable. 

  
## 2.3 
  
  Message Bus (which supports publisher / subscriber mode) architecture will be easy to scale up, and also be better isolation / modularization. It would be the best practice for this project.

  Covered the heavy workload scenario, set the retry mechanism, messages will be discarded when all retries fail. The persistent store of these messages can be considered to handle those failed messages, to provide a reliable message system, but it didn't implement in this approach. 

  
## 2.4 
  
  PriorityQueue satisfies the sorting requirements. It is suitable for implementing the FIFO strategy of matching order and courier.

  
## 2.5 
  
  UnitTest is implemented with JUnit framework.

  
## 2.6 
  
  Some Open Source libraries are introduced to this project: Log4J / SLF4J , UUID and dependencies.
  
  
