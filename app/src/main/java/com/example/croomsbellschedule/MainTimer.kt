import java.time.LocalDate
import java.time.LocalTime
import java.time.temporal.ChronoUnit

import kotlinx.coroutines.*
import org.json.JSONObject
import java.net.URL

fun mainTimerOnline(): String {

    var schedules: String = "null" // Use appropriate type for your schedule data
    suspend fun fetchAndParseSchedule(): Result<Unit> = withContext(Dispatchers.IO) {
        val url = URL("https://api.croomssched.tech/today")
        val connection = url.openConnection()
        val inputStream = connection.getInputStream()

        val text = inputStream.bufferedReader().use { it.readText() }
        inputStream.close() // Important: Close the stream

        val json = JSONObject(text)
        val data = json.get("data") // Or json.opt("data") for optional data

        schedules = data.toString() // Assign to your schedules variable
        Result.success(Unit) // Indicate success

                }
    return (schedules)
}


fun mainTimer(): String {
    //Gets Date
    val date = LocalDate.now().dayOfWeek.value
    val alunchAlt = false
    val aorblunch = "even"
    //Converts "aorblunch" to boolean "alunch"
    val alunch = if (aorblunch == "even"){
        true
    } else if (aorblunch == "odd"){
        false
    } else {
        throw IllegalArgumentException("Value must be even or odd")
    }
    //Gets time to next period
    when (date) {
        1 -> {return(normalBlock(alunch))}
        2 -> {return(normalBlock(alunch))}
        3 -> {return(evenBlock(alunchAlt))}
        4 -> {return(oddBlock(alunch))}
        5 -> {return(normalBlock(alunch))}
        6 -> {return(normalBlock(alunch))}
        7 -> {return(normalBlock(alunch))}
        }
    return("null")
    }


fun normalBlock (alunch: Boolean): String {
    //Gets time in hh:mm format
    val time = LocalTime.now().truncatedTo(ChronoUnit.MINUTES)
    //Defines period timing
    val p1 = LocalTime.of(7, 20)
    val p2 = LocalTime.of(8, 13)
    val p3 = LocalTime.of(9, 10)
    val p4 = LocalTime.of(10, 5)
    val p5 = if (alunch) { LocalTime.of(11, 32) } else { LocalTime.of(11,2) }
    val lunch = if (alunch) { LocalTime.of(11, 2) } else { LocalTime.of(11,57) }
    val p6 = LocalTime.of(12,27)
    val p7 = LocalTime.of(13,24)
    val afterschool = LocalTime.of(14, 20)
    //Returns the proper countdown to next period
    if (time < p1) {
        return ("School starts in " + ChronoUnit.MINUTES.between(time, p1) + " minutes").toString()
    } else if (time < p2){
        return ("Period 2 starts in " + ChronoUnit.MINUTES.between(time, p2) + " minutes").toString()
    } else if (time < p3){
        return ("Period 3 starts in " + ChronoUnit.MINUTES.between(time, p3) + " minutes").toString()
    } else if (time < p4){
        return ("Period 4 starts in " + ChronoUnit.MINUTES.between(time, p4) + " minutes").toString()
    } else if (alunch && time < lunch) {
        return ("Lunch starts in " + ChronoUnit.MINUTES.between(time, lunch) + " minutes").toString()
    } else if (alunch && time < p5){
        return ("Period 5 starts in " + ChronoUnit.MINUTES.between(time, p5) + " minutes").toString()
    } else if (!alunch && time < p5) {
        return ("Period 5 starts in " + ChronoUnit.MINUTES.between(time, lunch) + " minutes").toString()
    } else if (!alunch && time < lunch) {
        return ("Lunch starts in " + ChronoUnit.MINUTES.between(time, lunch) + " minutes").toString()
    } else if (time < p6){
        return ("Period 6 starts in " + ChronoUnit.MINUTES.between(time, p6) + " minutes").toString()
    } else if (time < p7){
        return ("Period 7 starts in " + ChronoUnit.MINUTES.between(time, p7) + " minutes").toString()
    } else if (time < afterschool){
        return ("School ends in " + ChronoUnit.MINUTES.between(time, afterschool) + " minutes").toString()
    } else if (time > afterschool){
        return ("School starts in " + ChronoUnit.HOURS.between(p1, time) + " hours").toString()
    }
    //Returns null if it fails somehow
    return ("null")
}

fun evenBlock (alunch: Boolean): String {
    //Gets time in hh:mm format
    val time = LocalTime.now().truncatedTo(ChronoUnit.MINUTES)
    //Defines period timing
    val p2 = LocalTime.of(7, 20)
    val p4 = LocalTime.of(8, 49)
    val homeroom = if (alunch) { LocalTime.of(10, 51) } else { LocalTime.of(10,21) }
    val lunch = if (alunch) { LocalTime.of(10, 21) } else { LocalTime.of(11,17) }
    val p6 = LocalTime.of(11, 47)
    val afterschool = LocalTime.of(13, 20)
    //Returns the proper countdown to next period
    if (time < p2) {
        return ("School starts in " + ChronoUnit.MINUTES.between(time, p2) + " minutes").toString()
    } else if (time < p4){
        return ("Period 4 starts in " + ChronoUnit.MINUTES.between(time, p4) + " minutes").toString()
    } else if (alunch && time < lunch) {
        return ("Lunch starts in " + ChronoUnit.MINUTES.between(time, lunch) + " minutes").toString()
    } else if (alunch && time < homeroom){
        return ("Homeroom starts in " + ChronoUnit.MINUTES.between(time, homeroom) + " minutes").toString()
    } else if (!alunch && time < homeroom) {
        return ("Homeroom starts in " + ChronoUnit.MINUTES.between(time, homeroom) + " minutes").toString()
    } else if (!alunch && time < lunch) {
        return ("Lunch starts in " + ChronoUnit.MINUTES.between(time, lunch) + " minutes").toString()
    } else if (time < p6){
        return ("Period 6 starts in " + ChronoUnit.MINUTES.between(time, p6) + " minutes").toString()
    } else if (time < afterschool){
        return ("School ends in " + ChronoUnit.MINUTES.between(time, afterschool) + " minutes").toString()
    } else if (time > afterschool){
        return ("School starts in " + ChronoUnit.HOURS.between(p2, time) + " hours").toString()
    }
    //Returns null if it fails somehow
    return("null")
}

fun oddBlock (alunch: Boolean): String {
    //Gets time in hh:mm format
    val time = LocalTime.now().truncatedTo(ChronoUnit.MINUTES)
    //Defines period timing
    val p1 = LocalTime.of(7, 20)
    val p3 = LocalTime.of(8, 55)
    val p5 = if (alunch) { LocalTime.of(11, 3) } else { LocalTime.of(10,33) }
    val lunch = if (alunch) { LocalTime.of(10, 33) } else { LocalTime.of(12,11) }
    val p7 = LocalTime.of(12, 41)
    val afterschool = LocalTime.of(14, 20)
    //Returns the proper countdown to next period
    if (time < p1) {
        return ("School starts in " + ChronoUnit.MINUTES.between(time, p1) + " minutes").toString()
    } else if (time < p3){
        return ("Period 4 starts in " + ChronoUnit.MINUTES.between(time, p3) + " minutes").toString()
    } else if (alunch && time < lunch) {
        return ("Lunch starts in " + ChronoUnit.MINUTES.between(time, lunch) + " minutes").toString()
    } else if (alunch && time < p5){
        return ("Homeroom starts in " + ChronoUnit.MINUTES.between(time, p5) + " minutes").toString()
    } else if (!alunch && time < p5) {
        return ("Homeroom starts in " + ChronoUnit.MINUTES.between(time, p5) + " minutes").toString()
    } else if (!alunch && time < lunch) {
        return ("Lunch starts in " + ChronoUnit.MINUTES.between(time, lunch) + " minutes").toString()
    } else if (time < p7){
        return ("Period 6 starts in " + ChronoUnit.MINUTES.between(time, p7) + " minutes").toString()
    } else if (time < afterschool){
        return ("School ends in " + ChronoUnit.MINUTES.between(time, afterschool) + " minutes").toString()
    } else if (time > afterschool){
        return ("School starts in " + ChronoUnit.HOURS.between(p1, time) + " hours").toString()
    }
    //Returns null if it fails somehow
    return("null")
}