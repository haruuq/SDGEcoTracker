package com.example.sdgecotracker

import android.app.AlertDialog
import android.app.DatePickerDialog
import android.content.ContentValues
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : AppCompatActivity() {

    private lateinit var dbHelper: DatabaseHelper
    private lateinit var recentAdapter: ConsumptionAdapter
    private lateinit var historyAdapter: ConsumptionAdapter
    private lateinit var userListAdapter: UserManagementAdapter

    private lateinit var spCategory: Spinner
    private lateinit var etItemName: EditText
    private lateinit var etQuantity: EditText
    private lateinit var etDate: EditText
    private lateinit var tvItemsCount: TextView
    private lateinit var tvEcoScore: TextView
    private lateinit var tvQuantityLabel: TextView
    private lateinit var tvUserSub: TextView
    private lateinit var tvAnalyticsSummary: TextView
    private lateinit var tvAdminStats: TextView
    private lateinit var btnAddEntry: Button
    private lateinit var chartCanvas: EcoChartView
    private lateinit var adminChartCanvas: AdminChartView
    private lateinit var tvWeekCompare: TextView
    private lateinit var rvUserList: RecyclerView

    private lateinit var layoutLogEntry: View
    private lateinit var layoutHistory: View
    private lateinit var layoutAnalytics: View
    private lateinit var layoutAdmin: View

    private lateinit var tabLogEntry: TextView
    private lateinit var tabHistory: TextView
    private lateinit var tabAnalytics: TextView
    private lateinit var tabAdmin: TextView

    private var selectedEntry: EcoEntry? = null
    private val calendar = Calendar.getInstance()
    private var currentUser: User? = null

    private val categoryUnits = mapOf(
        "Food" to "g",
        "Plastic" to "pcs",
        "Energy" to "kWh",
        "Transport" to "km",
        "Water" to "L"
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        dbHelper = DatabaseHelper(this)

        // Get logged-in user from intent
        val userId = intent.getIntExtra("USER_ID", -1)
        val userName = intent.getStringExtra("USER_NAME") ?: "User"
        val userRole = intent.getStringExtra("USER_ROLE") ?: "user"
        currentUser = User(userId, userName, userRole)

        initViews()
        setupCategorySpinner()
        setupDatePicker()
        setupRecyclerViews()
        setupButtonListeners()
        updateRoleUI()
        refreshAllData()
        switchTab(layoutLogEntry, tabLogEntry)
    }

    private fun initViews() {
        layoutLogEntry = findViewById(R.id.layoutLogEntry)
        layoutHistory = findViewById(R.id.layoutHistory)
        layoutAnalytics = findViewById(R.id.layoutAnalytics)
        layoutAdmin = findViewById(R.id.layoutAdmin)

        tabLogEntry = findViewById(R.id.tabLogEntry)
        tabHistory = findViewById(R.id.tabHistory)
        tabAnalytics = findViewById(R.id.tabAnalytics)
        tabAdmin = findViewById(R.id.tabAdmin)

        spCategory = findViewById(R.id.spCategory)
        etItemName = findViewById(R.id.etItemName)
        etQuantity = findViewById(R.id.etQuantity)
        etDate = findViewById(R.id.etDate)
        tvQuantityLabel = findViewById(R.id.tvQuantityLabel)
        tvUserSub = findViewById(R.id.tvUserSub)
        tvAnalyticsSummary = findViewById(R.id.tvAnalyticsSummary)
        tvAdminStats = findViewById(R.id.tvAdminStats)
        btnAddEntry = findViewById(R.id.btnAddEntry)
        tvItemsCount = findViewById(R.id.tvItemsCount)
        tvEcoScore = findViewById(R.id.tvEcoScore)
        tvWeekCompare = findViewById(R.id.tvWeekCompare)
        chartCanvas = findViewById(R.id.chartCanvas)
        adminChartCanvas = findViewById(R.id.adminChartCanvas)
        rvUserList = findViewById(R.id.rvUserList)
    }

    private fun setupCategorySpinner() {
        val categories = categoryUnits.keys.toTypedArray()
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, categories)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spCategory.adapter = adapter
        spCategory.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val unit = categoryUnits[categories[position]] ?: ""
                tvQuantityLabel.text = getString(R.string.quantity_label_with_unit, unit)
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }

    private fun setupDatePicker() {
        val listener = DatePickerDialog.OnDateSetListener { _, year, month, day ->
            calendar.set(Calendar.YEAR, year)
            calendar.set(Calendar.MONTH, month)
            calendar.set(Calendar.DAY_OF_MONTH, day)
            updateDateLabel()
        }
        etDate.setOnClickListener {
            DatePickerDialog(this, listener,
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)).show()
        }
        updateDateLabel()
    }

    private fun setupRecyclerViews() {
        recentAdapter = ConsumptionAdapter(mutableListOf(), ::onEditRequest, ::onDeleteRequest)
        historyAdapter = ConsumptionAdapter(mutableListOf(), ::onEditRequest, ::onDeleteRequest)
        userListAdapter = UserManagementAdapter(mutableListOf(), ::onEditUser, ::onDeleteUser)

        val rvRecent = findViewById<RecyclerView>(R.id.rvRecent)
        val rvHistory = findViewById<RecyclerView>(R.id.rvHistory)

        rvRecent.layoutManager = LinearLayoutManager(this)
        rvRecent.adapter = recentAdapter
        rvHistory.layoutManager = LinearLayoutManager(this)
        rvHistory.adapter = historyAdapter
        rvUserList.layoutManager = LinearLayoutManager(this)
        rvUserList.adapter = userListAdapter
    }

    private fun setupButtonListeners() {
        tabLogEntry.setOnClickListener { switchTab(layoutLogEntry, tabLogEntry) }
        tabHistory.setOnClickListener { switchTab(layoutHistory, tabHistory) }
        tabAnalytics.setOnClickListener { switchTab(layoutAnalytics, tabAnalytics) }
        tabAdmin.setOnClickListener { switchTab(layoutAdmin, tabAdmin) }

        btnAddEntry.setOnClickListener { saveEntry() }
        findViewById<Button>(R.id.btnCancel).setOnClickListener { resetFields() }
        findViewById<Button>(R.id.btnExportPdf).setOnClickListener { exportToPdf() }

        val btnEditFooter = findViewById<LinearLayout>(R.id.btnEditFooter)
        val btnDeleteFooter = findViewById<LinearLayout>(R.id.btnDeleteFooter)
        val btnReportFooter = findViewById<LinearLayout>(R.id.btnReportFooter)

        btnEditFooter.setOnClickListener {
            selectedEntry?.let { onEditRequest(it) }
                ?: Toast.makeText(this, "Select an item to edit", Toast.LENGTH_SHORT).show()
        }
        btnDeleteFooter.setOnClickListener {
            selectedEntry?.let { onDeleteRequest(it) }
                ?: Toast.makeText(this, "Select an item to delete", Toast.LENGTH_SHORT).show()
        }
        btnReportFooter.setOnClickListener { showReport() }

        // Admin: Add User button
        val btnAddUser = findViewById<Button>(R.id.btnAddUser)
        btnAddUser.setOnClickListener { showAddUserDialog() }

        // Logout
        val btnLogout = findViewById<LinearLayout>(R.id.btnLogout)
        btnLogout.setOnClickListener {
            finish()
        }
    }

    private fun updateRoleUI() {
        currentUser?.let { user ->
            tvUserSub.text = "Logged in as: ${user.username}  |  Role: ${user.role.uppercase()}"
            if (user.role == "admin") {
                tabAdmin.visibility = View.VISIBLE
            } else {
                tabAdmin.visibility = View.GONE
            }
        }
    }

    private fun switchTab(targetLayout: View, targetTab: TextView) {
        listOf(layoutLogEntry, layoutHistory, layoutAnalytics, layoutAdmin)
            .forEach { it.visibility = View.GONE }
        listOf(tabLogEntry, tabHistory, tabAnalytics, tabAdmin)
            .forEach { it.setBackgroundColor(Color.TRANSPARENT) }

        targetLayout.visibility = View.VISIBLE
        targetTab.setBackgroundResource(R.drawable.glossy_button)

        if (targetLayout == layoutAnalytics) updateAnalytics()
        if (targetLayout == layoutAdmin) updateAdminStats()
        if (targetLayout == layoutHistory) updateHistoryGrouped()
    }

    private fun updateDateLabel() {
        val sdf = SimpleDateFormat("MMM dd, yyyy", Locale.US)
        etDate.setText(sdf.format(calendar.time))
    }

    private fun refreshAllData() {
        val user = currentUser ?: return
        
        // Use a background thread for database operations to prevent UI freezing
        Thread {
            val entries = dbHelper.getAllEntries(if (user.role == "admin") null else user.id)
            val today = SimpleDateFormat("MMM dd, yyyy", Locale.US).format(Date())
            val count = dbHelper.getTodayCount(user.id, today)
            val score = dbHelper.getTodayScore(user.id, today)

            runOnUiThread {
                recentAdapter.updateData(entries.take(5))
                tvItemsCount.text = count.toString()
                tvEcoScore.text = score.toString()
                tvEcoScore.setTextColor(when {
                    score >= 100 -> Color.parseColor("#76B852")
                    score >= 50  -> Color.parseColor("#F9A825")
                    else         -> Color.parseColor("#FF5252")
                })
                EcoTrackerWidget.sendRefreshBroadcast(this)
            }
        }.start()
    }

    private fun updateHistoryGrouped() {
        val user = currentUser ?: return
        Thread {
            val entries = dbHelper.getAllEntries(user.id)
            runOnUiThread {
                historyAdapter.updateData(entries)
            }
        }.start()
    }

    private fun saveEntry() {
        val user = currentUser ?: return
        val category = spCategory.selectedItem.toString()
        val item = etItemName.text.toString().trim()
        val rawQuantity = etQuantity.text.toString().trim()
        val date = etDate.text.toString()

        if (item.isNotEmpty() && rawQuantity.isNotEmpty()) {
            val unit = categoryUnits[category] ?: ""
            val quantityWithUnit = "$rawQuantity $unit"
            val qty = rawQuantity.toDoubleOrNull() ?: 1.0
            val score = calculateScore(category, qty)

            if (selectedEntry == null) {
                dbHelper.addEntry(user.id, category, item, quantityWithUnit, date, score)
                Toast.makeText(this, getString(R.string.activity_added), Toast.LENGTH_SHORT).show()
            } else {
                dbHelper.updateEntry(selectedEntry!!.id, category, item, quantityWithUnit, date, score)
                Toast.makeText(this, getString(R.string.activity_updated), Toast.LENGTH_SHORT).show()
            }
            resetFields()
            refreshAllData()
        } else {
            Toast.makeText(this, getString(R.string.please_fill_all_fields), Toast.LENGTH_SHORT).show()
        }
    }

    private fun onEditRequest(entry: EcoEntry) {
        selectedEntry = entry
        etItemName.setText(entry.item)
        val unit = categoryUnits[entry.category] ?: ""
        etQuantity.setText(entry.quantity.removeSuffix(" $unit").trim())
        etDate.setText(entry.date)
        val pos = categoryUnits.keys.toTypedArray().indexOf(entry.category)
        if (pos >= 0) spCategory.setSelection(pos)
        btnAddEntry.text = getString(R.string.update_activity)
        switchTab(layoutLogEntry, tabLogEntry)
    }

    private fun onDeleteRequest(entry: EcoEntry) {
        AlertDialog.Builder(this)
            .setTitle("Delete Entry")
            .setMessage("Delete '${entry.item}'?")
            .setPositiveButton("Delete") { _, _ ->
                dbHelper.deleteEntry(entry.id)
                if (selectedEntry?.id == entry.id) resetFields()
                refreshAllData()
                updateHistoryGrouped()
                Toast.makeText(this, getString(R.string.deleted), Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun onEditUser(user: User) {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_add_user, null)
        val etName = dialogView.findViewById<EditText>(R.id.etNewUsername)
        val etPass = dialogView.findViewById<EditText>(R.id.etNewPassword)
        val spRole = dialogView.findViewById<Spinner>(R.id.spNewRole)

        val roles = arrayOf("user", "admin")
        val roleAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, roles)
        roleAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spRole.adapter = roleAdapter

        // Pre-fill existing values
        etName.setText(user.username)
        etPass.hint = "Leave blank to keep password"
        val roleIndex = roles.indexOf(user.role).takeIf { it >= 0 } ?: 0
        spRole.setSelection(roleIndex)

        AlertDialog.Builder(this)
            .setTitle("Edit User")
            .setView(dialogView)
            .setPositiveButton("Save") { _, _ ->
                val name = etName.text.toString().trim()
                val pass = etPass.text.toString().trim()
                val role = spRole.selectedItem.toString()
                if (name.isNotEmpty()) {
                    dbHelper.updateUser(user.id, name, pass.ifEmpty { null }, role)
                    updateAdminStats()
                    Toast.makeText(this, "User '$name' updated", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, "Username cannot be empty", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun onDeleteUser(user: User) {
        if (user.id == currentUser?.id) {
            Toast.makeText(this, "Cannot delete yourself", Toast.LENGTH_SHORT).show()
            return
        }
        AlertDialog.Builder(this)
            .setTitle("Delete User")
            .setMessage("Delete user '${user.username}' and all their data?")
            .setPositiveButton("Delete") { _, _ ->
                dbHelper.deleteUser(user.id)
                updateAdminStats()
                Toast.makeText(this, "User deleted", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showAddUserDialog() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_add_user, null)
        val etName = dialogView.findViewById<EditText>(R.id.etNewUsername)
        val etPass = dialogView.findViewById<EditText>(R.id.etNewPassword)
        val spRole = dialogView.findViewById<Spinner>(R.id.spNewRole)

        val roles = arrayOf("user", "admin")
        val roleAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, roles)
        roleAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spRole.adapter = roleAdapter

        AlertDialog.Builder(this)
            .setTitle("Add New User")
            .setView(dialogView)
            .setPositiveButton("Add") { _, _ ->
                val name = etName.text.toString().trim()
                val pass = etPass.text.toString().trim()
                val role = spRole.selectedItem.toString()
                if (name.isNotEmpty() && pass.isNotEmpty()) {
                    dbHelper.addUser(name, pass, role)
                    updateAdminStats()
                    Toast.makeText(this, "User '$name' added", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, "Fill all fields", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }


    private fun calculateScore(category: String, quantity: Double): Int {
        val penalty = when (category) {
            "Food"      -> quantity / 10.0
            "Plastic"   -> quantity * 3.0
            "Energy"    -> quantity * 1.0
            "Transport" -> quantity * 1.0
            "Water"     -> quantity * 1.0
            else        -> quantity * 1.0
        }
        return penalty.toInt().coerceIn(1, 30)
    }

    private fun resetFields() {
        selectedEntry = null
        etItemName.text.clear()
        etQuantity.text.clear()
        btnAddEntry.text = getString(R.string.add_entry)
        calendar.time = Date()
        updateDateLabel()
    }

    private fun updateAnalytics() {
        val user = currentUser ?: return
        
        Thread {
            val entries = dbHelper.getAllEntries(user.id)
            val thisWeek = dbHelper.getWeeklyAvgScore(user.id, 0)
            val lastWeek = dbHelper.getWeeklyAvgScore(user.id, 1)
            val dailyScores = dbHelper.getDailyScores(user.id)

            runOnUiThread {
                if (entries.isEmpty()) {
                    tvAnalyticsSummary.text = "No data available. Start logging consumption!"
                    tvWeekCompare.text = ""
                    chartCanvas.setData(emptyList())
                    return@runOnUiThread
                }

                val totalEntries = entries.size
                val common = entries.groupBy { it.category }.maxByOrNull { it.value.size }?.key ?: "None"
                
                // Compute daily eco scores (100 - SUM of penalties per day), then average across days
                val dailyEcoScores = entries.groupBy { it.date }
                    .mapValues { (_, v) -> (100 - v.sumOf { it.score }).coerceAtLeast(0) }
                val avgScore = if (dailyEcoScores.isEmpty()) 0 else dailyEcoScores.values.average().toInt()
                val bestDay = dailyEcoScores.maxByOrNull { it.value }
                val categoryCounts = entries.groupBy { it.category }.mapValues { it.value.size }
                
                val summary = buildString {
                    appendLine("📊 Your Eco Analytics")
                    appendLine()
                    appendLine("Overall Avg Score:  $avgScore")
                    appendLine("Total Entries Logged:  $totalEntries")
                    appendLine("Most Logged Category:  $common")
                    if (bestDay != null) appendLine("Best Day:  ${bestDay.key} (Score: ${bestDay.value})")
                    appendLine()
                    appendLine("Category Breakdown:")
                    categoryCounts.forEach { (cat, count) -> appendLine("  • $cat: $count entries") }
                }
                tvAnalyticsSummary.text = summary

                // Week comparison
                val diff = thisWeek - lastWeek
                val arrow = if (diff >= 0) "▲" else "▼"
                val color = if (diff >= 0) "#76B852" else "#FF5252"
                tvWeekCompare.text = "This week: $thisWeek  |  Last week: $lastWeek  $arrow ${kotlin.math.abs(diff)}"
                tvWeekCompare.setTextColor(Color.parseColor(color))

                // Line chart data (daily scores)
                chartCanvas.setData(dailyScores)
            }
        }.start()
    }

    private fun updateAdminStats() {
        // Run database calls on main thread temporarily to ensure list updates properly
        val impact = dbHelper.getCommunityImpact()
        val mostCommon = dbHelper.getMostCommonWasteCategory()
        val users = dbHelper.getAllUsers()
        val userScores = dbHelper.getUserScoreSummary()
        val catDist = dbHelper.getCategoryDistribution()

        val stats = buildString {
            appendLine("🌍 Community Impact")
            appendLine()
            appendLine("Total Registered Users: ${users.size}")
            appendLine("Most Common Waste Category: $mostCommon")
            appendLine()
            appendLine("Avg Eco Score by Category:")
            impact.forEach { (cat, score) -> appendLine("  • $cat: ${score.toInt()}") }
            appendLine()
            appendLine("Per-User Eco Scores:")
            userScores.forEach { (_, name, score) -> appendLine("  • $name: $score") }
        }
        tvAdminStats.text = stats

        // Pie-style bar chart for admin
        adminChartCanvas.setData(catDist)

        // Update user list recycler (filter out current admin to focus on manageable users)
        val manageableUsers = users.filter { it.id != currentUser?.id }
        userListAdapter.updateData(manageableUsers)
        
        Toast.makeText(this, "Displaying ${manageableUsers.size} manageable users", Toast.LENGTH_SHORT).show()
    }

    private fun showReport() {
        val user = currentUser ?: return
        val entries = dbHelper.getAllEntries(user.id)
        if (entries.isEmpty()) {
            Toast.makeText(this, "No data to report", Toast.LENGTH_SHORT).show()
            return
        }
        val avgDailyScore = entries.groupBy { it.date }
            .values.map { (100 - it.sumOf { e -> e.score }).coerceAtLeast(0) }
            .let { if (it.isEmpty()) 0 else it.average().toInt() }
        val thisWeek = dbHelper.getWeeklyAvgScore(user.id, 0)
        val lastWeek = dbHelper.getWeeklyAvgScore(user.id, 1)
        val categoryCounts = entries.groupBy { it.category }.mapValues { it.value.size }
        val msg = buildString {
            appendLine("User: ${user.username}")
            appendLine("Role: ${user.role.uppercase()}")
            appendLine()
            appendLine("Total Entries: ${entries.size}")
            appendLine("Overall Eco Score: $avgDailyScore")
            appendLine("This Week Avg: $thisWeek")
            appendLine("Last Week Avg: $lastWeek")
            appendLine()
            appendLine("Category Summary:")
            categoryCounts.forEach { (cat, count) -> appendLine("  • $cat: $count entries") }
        }
        AlertDialog.Builder(this)
            .setTitle("Sustainability Report")
            .setMessage(msg)
            .setPositiveButton("Export PDF") { _, _ -> exportToPdf() }
            .setNegativeButton("Close", null)
            .show()
    }

    private fun exportToPdf() {
        val user = currentUser ?: return
        val entries = dbHelper.getAllEntries(user.id)
        if (entries.isEmpty()) {
            Toast.makeText(this, "No data to export", Toast.LENGTH_SHORT).show()
            return
        }

        val document = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create() // A4
        val page = document.startPage(pageInfo)
        val canvas: Canvas = page.canvas
        val paint = Paint()

        // Header bar
        paint.color = Color.parseColor("#1A330D")
        canvas.drawRect(0f, 0f, 595f, 70f, paint)

        paint.color = Color.WHITE
        paint.textSize = 22f
        paint.isFakeBoldText = true
        canvas.drawText("EcoTrack – Sustainability Report", 20f, 30f, paint)
        paint.textSize = 13f
        paint.isFakeBoldText = false
        canvas.drawText("Generated: ${SimpleDateFormat("MMM dd, yyyy", Locale.US).format(Date())}", 20f, 55f, paint)

        // User info section
        paint.color = Color.parseColor("#347414")
        paint.textSize = 16f
        paint.isFakeBoldText = true
        canvas.drawText("User: ${user.username}  |  Role: ${user.role.uppercase()}", 20f, 95f, paint)

        paint.color = Color.parseColor("#1A330D")
        canvas.drawRect(20f, 100f, 575f, 102f, paint)

        // Eco score summary
        val avgDailyScore = entries.groupBy { it.date }
            .values.map { (100 - it.sumOf { e -> e.score }).coerceAtLeast(0) }
            .let { if (it.isEmpty()) 0 else it.average().toInt() }
        val thisWeek = dbHelper.getWeeklyAvgScore(user.id, 0)
        val lastWeek = dbHelper.getWeeklyAvgScore(user.id, 1)

        paint.color = Color.BLACK
        paint.textSize = 13f
        paint.isFakeBoldText = true
        canvas.drawText("Eco Score Summary", 20f, 125f, paint)
        paint.isFakeBoldText = false
        paint.textSize = 12f
        canvas.drawText("Overall Average Score: $avgDailyScore", 30f, 145f, paint)
        canvas.drawText("This Week Average: $thisWeek   |   Last Week Average: $lastWeek", 30f, 162f, paint)
        canvas.drawText("Total Entries Logged: ${entries.size}", 30f, 179f, paint)

        // Category breakdown
        paint.textSize = 13f
        paint.isFakeBoldText = true
        canvas.drawText("Category Breakdown", 20f, 205f, paint)
        paint.isFakeBoldText = false
        paint.textSize = 12f
        val catCounts = entries.groupBy { it.category }.mapValues { it.value.size }
        var catY = 222f
        catCounts.forEach { (cat, count) ->
            canvas.drawText("  $cat: $count entries", 30f, catY, paint)
            catY += 17f
        }

        // Log table
        paint.textSize = 13f
        paint.isFakeBoldText = true
        canvas.drawText("Weekly Log (Last 30 entries)", 20f, catY + 15f, paint)

        // Table header
        paint.color = Color.parseColor("#2D5A18")
        canvas.drawRect(20f, catY + 20f, 575f, catY + 36f, paint)
        paint.color = Color.WHITE
        paint.textSize = 10f
        paint.isFakeBoldText = true
        canvas.drawText("Date", 25f, catY + 33f, paint)
        canvas.drawText("Category", 120f, catY + 33f, paint)
        canvas.drawText("Item", 220f, catY + 33f, paint)
        canvas.drawText("Qty", 380f, catY + 33f, paint)
        canvas.drawText("Score", 470f, catY + 33f, paint)

        paint.color = Color.BLACK
        paint.isFakeBoldText = false
        var y = catY + 50f
        entries.take(30).forEach { entry ->
            if (y > 800f) return@forEach
            canvas.drawText(entry.date, 25f, y, paint)
            canvas.drawText(entry.category, 120f, y, paint)
            val itemTrunc = if (entry.item.length > 18) entry.item.take(18) + "…" else entry.item
            canvas.drawText(itemTrunc, 220f, y, paint)
            canvas.drawText(entry.quantity, 380f, y, paint)
            canvas.drawText(entry.score.toString(), 470f, y, paint)
            y += 15f
        }

        // Footer
        paint.color = Color.parseColor("#76B852")
        canvas.drawRect(0f, 810f, 595f, 842f, paint)
        paint.color = Color.WHITE
        paint.textSize = 10f
        canvas.drawText("SDG 12 – Responsible Consumption and Production  |  EcoTrack App", 20f, 830f, paint)

        document.finishPage(page)

        val fileName = "EcoReport_${user.username}_${System.currentTimeMillis()}.pdf"
        
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val resolver = contentResolver
                val contentValues = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                    put(MediaStore.MediaColumns.MIME_TYPE, "application/pdf")
                    put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
                }
                val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
                if (uri != null) {
                    resolver.openOutputStream(uri)?.use { document.writeTo(it) }
                    Toast.makeText(this, "PDF saved to Downloads folder", Toast.LENGTH_LONG).show()
                }
            } else {
                val dir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                val filePath = File(dir, fileName)
                document.writeTo(FileOutputStream(filePath))
                Toast.makeText(this, "PDF saved: ${filePath.absolutePath}", Toast.LENGTH_LONG).show()
            }
        } catch (e: Exception) {
            Toast.makeText(this, "Error exporting PDF: ${e.message}", Toast.LENGTH_SHORT).show()
        }
        document.close()
    }
}
