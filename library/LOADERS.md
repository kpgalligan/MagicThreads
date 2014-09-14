## loaders
 
The basic way loaders work. You do some thing in the background, the data is given to the foreground. It'll do some 
caching so rotations don't trigger reloads. You can also listen for data update events, and tell the loader to refresh.

That's the simple version. If you look at the code for loaders, things seem far more complex. There's really a lot
going on there.

For the most part, loaders are suggested to be used with ContentProvider. Unless you're sharing data outside of your
app, I've never been a fan. Long story. Anyway, you don't *need* to use ContentProvider for loaders, but if you don't, 
you need to do a lot of complex hand coding. This part of the lib attempts to avoid that.

### AbstractDataLoader

The top level parent. It provides the abstract methods:

findContent - Override this and do your data loading here.
handleError - Explicit error handling. Return false and crash, true and you've "handled" the Exception
registerContentChangedObserver - Register for updates (generally let a subclass implement this)
unregisterContentChangedObserver - Unregister for updates (generally let a subclass implement this)

Generally you don't extend AbstractDataLoader directly.

### AbstractEventBusLoader

Registers to EventBus for update notifications.  You'll need to provide your own "onEvent" methods.

### AbstractLocalBroadcastReceiverLoader

Use with LocalBroadcastManager

### AbstractSmoothLocalBroadcastReceiverLoader

If you're expecting multiple notifications in a short period, this will smooth them out.

### AbstractDoubleTapLoader

This is basic in concept, but kind of complex in implementation.  Basically, if you have data that is coming from 
the web, but may be locally cached, this will be good for you.  It'll load local, kick off remote loading, and 
update as either come in.  The rules are sort of complex. See example (https://github.com/touchlab/MagicThreadsDemo).

The loader takes two args in the generic declaration. The first is the type of data, like the other loaders.  The 
second is the type of error response. This can be whatever you want, but suggestions would be string or int codes 
from the server that can be translated into string resource references in some manner.  Can whatever you want, though.

For a list of SomeData and using an int for errors...

public class SomeDataLoader extends AbstractDoubleTapEventBusLoader<List<SomeData>, Integer>

The methods to implement are:

List<SomeData> findLocalContent() - Grab the local cache content. Return null if none, or if you've decided its too old (or whatever).

Integer findRemoteContent() - Grab the remote content. This is a bit complex. Grab the remote content and save it in your local cache.
The return value should be null if everything is OK.  If there's an error, return the error (it should match the declared error type).
To be clear, you need to save your own content locally.  findLocalContent will be called again.

Future extensions will have a way of distinguishing if the remote data has actually changed from local, but its still pretty basic.

#### Double tap callbacks

The other complex part of this is the loader callbacks.

new LoaderManager.LoaderCallbacks<DoubleTapResult<List<SomeData>, Integer>>()

There's a new type, DoubleTapResult. It takes the same generics as the loader:

DoubleTapResult<D, E>

In our example

DoubleTapResult<List<SomeData>, Integer>

The result class has three members:

private final Status status;
private final D result;
private final E error;

The status tells you where things are currently at:

public enum Status
{
    Data, NoData, Waiting, Error
}

Data - You have data.  Show it, and assume you're done, unless another Data message comes in.

NoData - You had no local data, and the remote call returned no data.

Waiting - You had no local data, and the remote call is processing.

Error - You had no local data, and the remote call had some kind of error.

If you have local data, you won't get Waiting or Error.  You *may* get another Data update, if the remote had
data, or NoData, if your remote call cleared out the local data.

In 'onLoadFinished', run a switch to see what the status is:

switch (result.getStatus())
{
    case Waiting:
        showWaiting();
        break;
    case Data:
        showResults(result.getResult());
        break;
    case NoData:
        //Show no data
        break;
    case Error:
        //Show error
        break;
    default:
        throw new UnsupportedOperationException("Weird");
}

This is fairly experimental, so there are some weird cases to sort out.  If local cache returns an empty list, that's funcationally the same
as NoData, but your code will need to distinguish that.  Alternatively, return null if your list is empty from findLocalContent.

You'll need to catch network exceptions yourself, and return an appropriate error from findRemoteContent.  If you had local data, and
don't clear it out on network errors, you won't get an error message to the callback.