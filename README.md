1. Usage


1.1 The runnable package is in the cli.zip . 

Run start.sh with -h option to print usage as below:

usage: -o <number> -ops <number> -r <y|n> -t <1|2|3>
-o <number>   --> Total number of orders, default is 100, range from 1 to 100,000.
-ops <Number> --> Order per second, default is 2, range from 1 to 100.
-r <y|n>      --> Randomly choosing food for orders, otherwise CheesePizza wil be chosen, default is [y]es.
-t <1|2|3>    --> Match type, 1: MATCH, 2: FIFO, 3: both 1 & 2 , default is 3.

Run start.sh without option to invoke the system with total 100 orders and ops 2. The output will print to both the console and a logfile "cloudkitchen.log".

1.2 Source code is in the code.zip . IntellJ IDEA is used for this project.


2. Design justification


2.1 Adopting multi-threading technology, to simulate multiple roles in a realtime system.

2.2 Adopting interface based programming technology, to simplify the structure, and make the system easy to extend. Total lines in this approach are less than 1000 .

2.3 Message Bus (which supports publisher / subscriber mode) architecture will be easy to scale up, and also be better isolation / modularization. It would be the best architecture for this project.

2.4 PriorityQueue satisfies both the sorting and the first-in-first-out requirements. It is suitable to implement the FIFO strategy for matching order and courier.

2.5 UnitTest is implemented with JUnit framework.

2.6 Some Open Source libraries are introduced to this project: Log4J / SLF4J , UUID and dependencies.