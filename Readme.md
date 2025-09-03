# Project Gretel
The goal of this project is to create an app that can allow users to track themselves when they have
no cell connection to backtrack back to where they started.  The primary design of this app has 3 states:

### Idle 
In this state the app does nothing and simply allows users to proceed to either of the next two states.

### Tracking
In this state, the app continually uses the GPS to track where the user is to create
a "trail of breadcrumbs" that is saved to an internal SQLite database.  While in this state,
the app should alert the user every 30 minutes that the app is in tracking mode.

### Following
In this state, the app picks the closest point as the starting point and the farthest
point as the target point, then using Dijkstra's algorithm, picks a path using the in-between
points.  The app will then display an arrow using the compass to show the user what direction
they should travel to reach the next point.  The "compass" should track the next point when
the user gets close to the next point.  If the app is closed or slept, then reawoken or opened,
the app should maintain the last endpoint but re-path from the nearest point.  This state should
automatically end if the user is too far from any point in the path or they are "on top" of the
last point.

