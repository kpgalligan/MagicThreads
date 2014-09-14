package co.touchlab.magicthreadsdemo.data;

import java.util.Date;

/**
 * Created by kgalligan on 9/13/14.
 */
public class DemoSimpleEvent
{
    public String name;
    public String description;
    public String venueName;
    public long startDate;
    public long endDate;

    @Override
    public String toString()
    {
        return name + " " + new Date(startDate).toString();
    }
}
