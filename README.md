MagicThreads is a threading support library for Android.  Its mostly for internal work at TouchLab, so you'll find the
docs kind of lacking, but its good stuff.

Demo project is in a different repo.  Intellij doesn't like gradle android projects.  Studio didn't seem to like 
the java project.

https://github.com/touchlab/MagicThreadsDemo

## tasks

TaskQueue is a background task executor. Its conceptually similar to running tasks in an executor service,
but has a few features that stick out. Tasks run in the background, but orchestration is done with the main thread.
The benefit is a long story, but in summary, it helps with UI modification. Also, you can query the live queue.
The main use case is to start a long running process, and enable/disable your input based on the live state of the queue
itself. Its safer than using surrogate boolean state, and makes rotation support easier (IMHO. YMMV).

[Persisted Task Queue](https://github.com/touchlab/MagicThreads/blob/master/library/docs/PERSISTED_QUEUE.md)

## eventbus

A minor extension on EventBus from green robot. By default, EventBus will push errors onto the bus
itself. You need to remember to catch them, then do something. That's what I'd call a pit of failure,
as you need to remember to do this, and people are bad at remembering things.

EventBusExt basically throws exceptions when they happen. Make app crash. Fix.

## loaders

A suite of loaders. I spent a lot of time trying to get away from loaders. They seem to complicated.
However, as I went along "fixing" them, I found out why most of that complexity existed, and have
come around full circle. Assuming you aren't recreating the way the UI works (Square with Mortar/Flow
for example), Loaders work well.

[More Loaders Info](https://github.com/touchlab/MagicThreads/blob/master/LOADERS.md)

## utils

UiThreadContext lets you assert if you're in the main thread or not in the main thread. I sometimes put that into
methods where the calling context isn't entirely clear.

## errorcontrol

This is in development. The basic idea is there are 2 types of exceptions in this world:
hard and soft. Soft happen sometimes, due to current conditions (basically network problems).
Hard is everything else. You *shouldn't* have hard exceptions, and in general I prefer
to let the app crash from unknown hard exceptions. Fail and fix.
