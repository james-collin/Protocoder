/*
*	Timer Executes a task repeatedly 
*   Delay Delays a function for a especific time 
*/

android.loop(1000, function () { 
    console.log("repeating every 1000 ms");
}); 

android.loop(5000, function () { 
    console.log("repeating every 5000 ms");
});

android.delay(1000, function() {
   console.log("delayed 1000 ms"); 
});


android.delay(2000, function() {
   console.log("delayed 2000 ms"); 
}); 
