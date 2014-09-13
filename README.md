MagicThreads is a threading support library for Android.  Its mostly for internal work at TouchLab, so you'll find the
docs kind of lacking, but its good stuff.

## errorcontrol

This is in development. The basic idea is there are 2 types of exceptions in this world:
hard and soft. Soft happen sometimes, due to current conditions (basically network problems).
Hard is everything else. You *shouldn't* have hard exceptions, and in general I prefer
to let the app crash from unknown hard exceptions. Fail and fix.

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

## tasks

BsyncTask works like AsyncTask, but attempts to support rotations, and aligning a reference to a caller
isn't done by carrying a reference to the caller around.  It "works", but its experimental, and I find
it kind of hacky. May go away.

TaskQueue is a background task executor. Its conceptually similar to running tasks in an executor service,
but has a few features that stick out. Tasks run in the background, but orchestration is done with the main thread.
The benefit is a long story, but in summary, it helps with UI modification. Also, you can query the live queue.
The main use case is to start a long running process, and enable/disable your input based on the live state of the queue
itself. Its safer than using surrogate boolean state, and makes rotation support easier (IMHO. YMMV).

## utils

UiThreadContext lets you assert if you're in the main thread or not in the main thread. I sometimes put that into
methods where the calling context isn't entirely clear.

### loaders
 
The basic way loaders work. You do some thing in the background, the data is given to the foreground. It'll do some 
caching so rotations don't trigger reloads. You can also listen for data update events, and tell the loader to refresh.

That's the simple version. If you look at the code for loaders, things seem far more complex. There's really a lot
going on there.

For the most part, loaders are suggested to be used with ContentProvider. Unless you're sharing data outside of your
app, I've never been a fan. Long story. Anyway, you don't *need* to use ContentProvider for loaders, but if you don't, 
you need to do a lot of complex hand coding. This part of the lib attempts to avoid that.

#### AbstractDataLoader

The top level parent. It provides the abstract methods:

findContent - Override this and do your data loading here.
handleError - Explicit error handling. Return false and crash, true and you've "handled" the Exception
registerContentChangedObserver - Register for updates (generally let a subclass implement this)
unregisterContentChangedObserver - Unregister for updates (generally let a subclass implement this)

Generally you don't extend AbstractDataLoader directly.

#### AbstractEventBusLoader

Registers to EventBus for update notifications.  You'll need to provide your own "onEvent" methods.

#### AbstractLocalBroadcastReceiverLoader

Use with LocalBroadcastManager

#### AbstractSmoothLocalBroadcastReceiverLoader

If you're expecting multiple notifications in a short period, this will smooth them out.

#### AbstractDoubleTapLoader

This is basic in concept, but kind of complex in implementation.  Basically, if you have data that is coming from 
the web, but may be locally cached, this will be good for you.  It'll load local, kick off remote loading, and 
update as either come in.  The rules are sort of complex. See example (when finished, which its not).