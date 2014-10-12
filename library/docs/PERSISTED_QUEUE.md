# Persisted Task Queue

The persisted task queue works similar to the standard task queue, except that all 
calls are persisted to disk before being run.  Tasks will run in order, the queue 
will "sleep" if the environment is not favorable (generally network issues), and
the queue will survive app restarts.

You would use a PTQ when you want to make sure a task is run, even if it takes a while.

The simplest use case is when you want to submit some data to a server, and if the calls
fails (temporarily), or if you're offline for long periods, that you call will eventually
get sent.

## Basic Setup

The simplest way to use the PTQ is to call DefaultPersistedTaskQueue.getInstance, and 
call 'execute'.  This will create a single PTQ, with its own SQLite database.

This will work, but if you're using your own database, there are good reasons to use 
your database to store commands.  You can do this by creating your own PTQ with config
pointing at your own DB.

## General Design

Like the standard TaskQueue, PTQ uses the Android main thread to orchestrate tasks.  This
is more precise, and simpler to build, although in heavy use, will have some level of 
performance trade off.

Storage operations and the actual task itself run in a single background thread.  Again,
if your app has significant performance requirements, this architecture may not be for you.
Great care has been taken to make sure tasks execute in a precise temporal order, at the 
cost of potential parallel performance gains.

Multithreading is complex, and potentially dangerous.  Precision and simplicity is often
more valuable than performance.  The apps we build with these tools generally need to 
record offline operations and perform them in a specific order.  If you just need to 
upload a bunch of stuff in any order, this is probably not your architecture.

However, you can create multiple queues if you have different classes of operations.  For
example, if you're syncing data, but also uploading large files, and the files can be sent
independently of the data, you could create a separate queue.  The data would be sent 
significantly faster than the files.  Bear in mind, each queue has its own database, 
so don't create lots of queues unless you know what you're doing.



