package co.touchlab.magicthreadsdemo.data;

import java.util.List;

import retrofit.client.Response;
import retrofit.http.GET;
import retrofit.http.Path;

/**
 * Created by kgalligan on 9/13/14.
 */
public interface EventRequest
{
    @GET("/dataTest/demoAppAllEvents/{conventionId}")
    List<DemoSimpleEvent> demoAppAllEvents(@Path("conventionId") Long conventionId);
}
