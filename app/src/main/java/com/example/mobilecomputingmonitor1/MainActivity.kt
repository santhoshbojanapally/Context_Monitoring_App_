package com.example.mobilecomputingmonitor1.ui

import com.example.mobilecomputingmonitor1.R
import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.room.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.BufferedReader
import java.io.InputStreamReader

// ---------------------------
// Room DB Setup
// ---------------------------
@Entity
data class HealthRecord(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val heartRate: Int,
    val respiratoryRate: Int,
    val symptom1: Int,
    val symptom2: Int,
    val symptom3: Int,
    val symptom4: Int,
    val symptom5: Int,
    val symptom6: Int,
    val symptom7: Int,
    val symptom8: Int,
    val symptom9: Int,
    val symptom10: Int
)

@Dao
interface HealthRecordDao {
    @Insert
    fun insert(record: HealthRecord)

    @Query("DELETE FROM HealthRecord")
    fun deleteAll()

    @Query("SELECT * FROM HealthRecord")
    fun getAllRecords(): List<HealthRecord>
}

@Database(entities = [HealthRecord::class], version = 1)
abstract class AppDatabase : RoomDatabase() {
    abstract fun healthRecordDao(): HealthRecordDao
}

// ---------------------------
// Main Activity
// ---------------------------
class MainActivity : ComponentActivity() {
    private lateinit var db: AppDatabase

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        db = Room.databaseBuilder(
            applicationContext,
            AppDatabase::class.java,
            "health-db"
        ).build()

        setContent {
            var screen by remember { mutableStateOf("home") }
            var hr by remember { mutableStateOf(0) }
            var rr by remember { mutableStateOf(0) }

            when (screen) {
                "home" -> HomeScreen(
                    onRecord = { screen = "measure" },
                    onView = { screen = "history" },
                    onDelete = {
                        Thread { db.healthRecordDao().deleteAll() }.start()
                    }
                )

                "measure" -> MeasurementScreen(
                    onFinish = { heartRate, respRate ->
                        hr = heartRate
                        rr = respRate
                        screen = "symptoms"
                    }
                )

                "symptoms" -> SymptomsScreen(
                    onSave = { ratings ->
                        val record = HealthRecord(
                            heartRate = hr,
                            respiratoryRate = rr,
                            symptom1 = ratings[0],
                            symptom2 = ratings[1],
                            symptom3 = ratings[2],
                            symptom4 = ratings[3],
                            symptom5 = ratings[4],
                            symptom6 = ratings[5],
                            symptom7 = ratings[6],
                            symptom8 = ratings[7],
                            symptom9 = ratings[8],
                            symptom10 = ratings[9]
                        )
                        Thread { db.healthRecordDao().insert(record) }.start()
                        screen = "home"
                    }
                )

                "history" -> HistoryScreen(db = db, onBack = { screen = "home" })
            }
        }
    }
}

// ---------------------------
// Composables
// ---------------------------
@Composable
fun HomeScreen(onRecord: () -> Unit, onView: () -> Unit, onDelete: () -> Unit) {
    Column(
        Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center
    ) {
        Button(onClick = onRecord, modifier = Modifier.fillMaxWidth()) {
            Text("Record Health Data")
        }
        Spacer(Modifier.height(16.dp))
        Button(onClick = onView, modifier = Modifier.fillMaxWidth()) {
            Text("View History")
        }
        Spacer(Modifier.height(16.dp))
        Button(onClick = onDelete, modifier = Modifier.fillMaxWidth()) {
            Text("Delete All Records")
        }
    }
}

@Composable
fun MeasurementScreen(onFinish: (Int, Int) -> Unit) {
    var heartRate by remember { mutableStateOf(0) }
    var respRate by remember { mutableStateOf(0) }
    val context = LocalContext.current

    Column(
        Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center
    ) {
        // -------------------
        // Heart rate from video
        // -------------------
        Button(onClick = {
            try {
                val videoUri = Uri.parse("android.resource://${context.packageName}/${R.raw.heart_rate}")
                heartRate = HeartRateHelper.getHeartRateFromVideo(context, videoUri)
            } catch (e: Exception) {
                Log.e("MeasurementScreen", "Heart rate measurement failed", e)
            }
        }) {
            Text("Start Heart Rate Measurement")
        }

        Spacer(Modifier.height(16.dp))

        // -------------------
        // Respiratory rate from CSV
        // -------------------
        Button(onClick = {
            try {
                val xData = readCsvToArray(context, "CSVBreatheX.csv")
                val yData = readCsvToArray(context, "CSVBreatheY.csv")
                val zData = readCsvToArray(context, "CSVBreatheZ.csv")

                respRate = RespiratoryRateHelper.getRespRateFromCsv(xData, yData, zData)
            } catch (e: Exception) {
                Log.e("MeasurementScreen", "Respiratory rate measurement failed", e)
            }
        }) {
            Text("Start Respiratory Rate Measurement")
        }

        Spacer(Modifier.height(24.dp))

        Text("Heart Rate: $heartRate bpm")
        Text("Respiratory Rate: $respRate breaths/min")

        Spacer(Modifier.height(24.dp))

        Button(onClick = { onFinish(heartRate, respRate) }) {
            Text("Save & Continue")
        }
    }
}

@Composable
fun SymptomsScreen(onSave: (List<Int>) -> Unit) {
    val symptomList = listOf(
        "Nausea", "Headache", "Diarrhea", "Sore Throat", "Fever",
        "Muscle Ache", "Loss of Smell or Taste", "Cough",
        "Shortness of Breath", "Feeling tired"
    )
    val ratings = remember { mutableStateListOf(0,0,0,0,0,0,0,0,0,0) }

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Text("Symptom Logging Page", style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(16.dp))

        LazyColumn {
            items(symptomList.indices.toList()) { i ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(symptomList[i])
                    Row {
                        (1..5).forEach { star ->
                            IconButton(onClick = { ratings[i] = star }) {
                                Icon(
                                    imageVector = if (star <= ratings[i])
                                        Icons.Filled.Star
                                    else Icons.Outlined.StarBorder,
                                    contentDescription = null
                                )
                            }
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
            }
        }

        Spacer(Modifier.height(16.dp))
        Button(onClick = { onSave(ratings) }, modifier = Modifier.fillMaxWidth()) {
            Text("Upload Symptoms")
        }
    }
}

@Composable
fun HistoryScreen(db: AppDatabase, onBack: () -> Unit) {
    var records by remember { mutableStateOf(listOf<HealthRecord>()) }
    val scope = rememberCoroutineScope()

    val symptomList = listOf(
        "Nausea", "Headache", "Diarrhea", "Sore Throat", "Fever",
        "Muscle Ache", "Loss of Smell or Taste", "Cough",
        "Shortness of Breath", "Feeling tired"
    )

    LaunchedEffect(Unit) {
        scope.launch(Dispatchers.IO) {
            records = db.healthRecordDao().getAllRecords()
        }
    }

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Text("History", style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(12.dp))

        if (records.isEmpty()) {
            Text("No records found.")
        } else {
            LazyColumn {
                items(records) { record ->
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
                    ) {
                        Column(Modifier.padding(16.dp)) {
                            Text("Vitals", style = MaterialTheme.typography.titleMedium)
                            Text("Heart Rate: ${record.heartRate} bpm")
                            Text("Respiratory Rate: ${record.respiratoryRate} breaths/min")

                            Spacer(Modifier.height(12.dp))
                            Text("Symptoms", style = MaterialTheme.typography.titleMedium)

                            val values = listOf(
                                record.symptom1, record.symptom2, record.symptom3,
                                record.symptom4, record.symptom5, record.symptom6,
                                record.symptom7, record.symptom8, record.symptom9,
                                record.symptom10
                            )

                            for (i in symptomList.indices) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(symptomList[i])
                                    Text(values[i].toString())
                                }
                            }
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(16.dp))
        Button(onClick = onBack, modifier = Modifier.fillMaxWidth()) {
            Text("Back to Home")
        }
    }
}

// ---------------------------
// CSV Reader Helper
// ---------------------------
fun readCsvToArray(context: Context, fileName: String): FloatArray {
    val inputStream = context.assets.open(fileName)
    val reader = BufferedReader(InputStreamReader(inputStream))
    val list = mutableListOf<Float>()

    reader.forEachLine { line ->
        val value = line.trim().toFloatOrNull()
        if (value != null) list.add(value)
    }

    reader.close()
    return list.toFloatArray()
}

// ---------------------------
// Helper Objects
// ---------------------------
object RespiratoryRateHelper {
    fun getRespRateFromCsv(x: FloatArray, y: FloatArray, z: FloatArray): Int {
        // TODO: Replace with your analysis logic
        val totalSamples = minOf(x.size, y.size , z.size)
        val breaths =totalSamples / 200
        val breathsPerMinute = breaths * (60 / 45)
        Log.d("RespiratoryRateHelper","Samples=$totalSamples -> Breaths=$breaths -> BPM=$breathsPerMinute")

        Log.d("RespiratoryRateHelper", "X:${x.size}, Y:${y.size}, Z:${z.size}")
        return breathsPerMinute // placeholder
    }
}

object HeartRateHelper {
    fun getHeartRateFromVideo(context: Context, uri: Uri): Int {
        // TODO: Replace with your video processing logic
        Log.d("HeartRateHelper", "Video URI: $uri")
        return 75 // placeholder
    }
}