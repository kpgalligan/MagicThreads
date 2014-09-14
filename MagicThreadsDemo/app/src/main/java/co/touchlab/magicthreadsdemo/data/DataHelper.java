package co.touchlab.magicthreadsdemo.data;

import android.content.Context;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import co.touchlab.android.threading.errorcontrol.NetworkException;
import co.touchlab.magicthreadsdemo.R;
import retrofit.ErrorHandler;
import retrofit.RequestInterceptor;
import retrofit.RestAdapter;
import retrofit.RetrofitError;
import retrofit.android.AndroidLog;
import retrofit.converter.GsonConverter;

/**
 * Created by kgalligan on 9/13/14.
 */
public class DataHelper
{
    public static RestAdapter.Builder makeRequestAdapterBuilder(Context context)
    {
        return makeRequestAdapterBuilder(context, new RetrofitErrorHandler());
    }

    public static RestAdapter.Builder makeRequestAdapterBuilder(final Context context, ErrorHandler errorHandler)
    {
        RequestInterceptor requestInterceptor = new RequestInterceptor()
        {
            @Override
            public void intercept(RequestFacade request)
            {
                request.addHeader("Accept", "application/json");
            }
        };

        Gson gson = new Gson();

        GsonConverter gsonConverter = new GsonConverter(gson);
        String baseURL = context.getString(R.string.base_url);

        RestAdapter.Builder builder = new RestAdapter.Builder().setRequestInterceptor(requestInterceptor).setConverter(gsonConverter).setLogLevel(RestAdapter.LogLevel.FULL).setLog(new AndroidLog("SquadUpApp")).setEndpoint(baseURL);

        if (errorHandler != null)
            builder.setErrorHandler(errorHandler);

        return builder;
    }

    public static class RetrofitErrorHandler implements ErrorHandler
    {
        @Override
        public Throwable handleError(RetrofitError cause)
        {
            if (cause.isNetworkError())
            {
                //getCause doesn't appear to actually return a "cause". Bummer.
                return new NetworkException(cause.getCause());
            }

            return cause;
        }
    }
}
