server sent events with Play
============================

This project was written to sustain a akka-user post (https://groups.google.com/forum/#!topic/akka-user/tUEqBwgrJYQ) 

Launch the code:

    ./activator ~run

This project expose 3 ways to generate SSE:

 * **WORKS** `http://locahost:9000/ticks` shoots texts via `Source.tick`
 * **FAILS** `http://localhost:9000/actor-flow` does not shoot two messages via a akkastream Source.actorRef
 * **SOLUTION** `http://localhost:9000/actor-flow-ok` shoots two messages via a akkastream Source.actorRef


Check with curl:

    octomaa:tmp alex$  curl  -H "Accept: text/event-stream" http://localhost:9000/ticks
    data: TICK
    
    data: TICK
    
    data: TICK
    ^C
    
    
    octomaa:tmp alex$  curl  -H "Accept: text/event-stream" http://localhost:9000/actor-flow
    ^C    
    
    octomaa:tmp alex$  curl  -H "Accept: text/event-stream" http://localhost:9000/actor-flow-ok
    data: PAF
    
    data: delayed PIF
    
    ^C

**Problem** : The second method sees the messages going through the akkastream flow but are not sent through the http channel...

**Solution**: The problem was having two flow and exposing the wrong materialization (thanks Viktor).
It is solved in the actorFlowWorking action.

All the code is in `app/controllers/StreamController`