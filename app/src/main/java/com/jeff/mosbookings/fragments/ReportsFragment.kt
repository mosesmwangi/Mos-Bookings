package com.jeff.mosbookings.fragments

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.*
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.github.mikephil.charting.formatter.ValueFormatter
import com.jeff.mosbookings.R
import com.jeff.mosbookings.adapters.ReportsAdapter
import com.jeff.mosbookings.databinding.FragmentReportsBinding
import com.jeff.mosbookings.models.BookingData
import com.jeff.mosbookings.models.RoomData
import com.jeff.mosbookings.repository.RoomRepository
import com.jeff.mosbookings.dialogs.AIAssistantDialog
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*
import com.itextpdf.kernel.pdf.PdfDocument
import com.itextpdf.kernel.pdf.PdfWriter
import com.itextpdf.layout.Document
import com.itextpdf.layout.element.Paragraph
import com.itextpdf.layout.element.Table
import com.itextpdf.layout.element.Cell
import com.itextpdf.layout.properties.TextAlignment
import com.itextpdf.layout.properties.UnitValue
import com.itextpdf.kernel.colors.ColorConstants
import com.itextpdf.layout.element.Div
import com.itextpdf.layout.properties.HorizontalAlignment

class ReportsFragment : Fragment() {
    private lateinit var binding: FragmentReportsBinding
    private lateinit var reportsAdapter: ReportsAdapter
    private val roomRepository = RoomRepository()
    private var allBookings: List<BookingData> = emptyList()
    private var allRoomMap: Map<String, RoomData> = emptyMap()
    private val TAG = "ReportsFragment"
    private val STORAGE_PERMISSION_CODE = 1001

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentReportsBinding.inflate(inflater, container, false)
        Log.d(TAG, "📊 ReportsFragment - onCreateView called")
        setupRecyclerView()
        loadReports()
        setupFabClickListener()
        return binding.root
    }

    private fun setupRecyclerView() {
        reportsAdapter = ReportsAdapter(emptyList())
        binding.reportsRecyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = reportsAdapter
        }
    }

    private fun loadReports() {
        Log.d(TAG, "📊 Loading reports...")
        binding.loadingProgressBar.visibility = View.VISIBLE
        
        val prefs = requireContext().getSharedPreferences("auth", android.content.Context.MODE_PRIVATE)
        val token = prefs.getString("jwt", null)
        
        Log.d(TAG, "📊 Token exists: ${token != null}")
        
        if (token == null) {
            binding.loadingProgressBar.visibility = View.GONE
            Toast.makeText(requireContext(), "Please login to view reports", Toast.LENGTH_SHORT).show()
            return
        }

        lifecycleScope.launch {
            try {
                val bookings = roomRepository.getAllBookings(token)
                val rooms = roomRepository.getRooms()
                val roomMap = rooms?.associateBy { it.id } ?: emptyMap()
                
                allBookings = bookings
                allRoomMap = roomMap
                
                if (isAdded && view != null) {
                    Log.d(TAG, "📊 Data loaded successfully - Bookings: ${bookings.size}, Rooms: ${rooms?.size ?: 0}")
                    val reportData = generateReportData(bookings)
                    Log.d(TAG, "📊 Generated ${reportData.size} report items")
                    
                    if (reportData.isNotEmpty()) {
                        reportsAdapter.updateReports(reportData)
                        setupCharts() // Setup charts with real data
                        showContent()
                    } else {
                        showEmptyState("No Data Available", "There's no booking data to generate reports yet.")
                    }
                    binding.loadingProgressBar.visibility = View.GONE
                }
            } catch (e: Exception) {
                if (isAdded && view != null) {
                    Toast.makeText(requireContext(), "Error loading reports: ${e.message}", Toast.LENGTH_SHORT).show()
                    binding.loadingProgressBar.visibility = View.GONE
                }
            }
        }
    }

    private fun generateReportData(bookings: List<BookingData>): List<ReportItem> {
        val reportItems = mutableListOf<ReportItem>()
        
        Log.d(TAG, "📊 Generating report data for ${bookings.size} bookings")
        
        // Get current user info
        val currentUser = getCurrentUserInfo()
        val currentUserId = getCurrentUserId()
        
        // Filter bookings for current user only
        val userBookings = if (currentUserId.isNotEmpty()) {
            bookings.filter { it.user == currentUserId }
        } else {
            bookings
        }
        
        Log.d(TAG, "📊 User-specific bookings: ${userBookings.size}")
        
        // ========== GENERAL SYSTEM ANALYTICS ==========
        
        // System-wide total bookings
        reportItems.add(ReportItem("Total System Bookings", bookings.size.toString(), R.drawable.ic_calendar))
        
        // Most popular room system-wide
        if (bookings.isNotEmpty()) {
            val systemRoomBookingCounts = bookings.groupingBy { it.roomName }.eachCount()
            val systemMostPopularRoom = systemRoomBookingCounts.maxByOrNull { it.value }
            if (systemMostPopularRoom != null) {
                reportItems.add(ReportItem("Most Popular Room", "${systemMostPopularRoom.key} (${systemMostPopularRoom.value})", R.drawable.ic_location))
            }
        }
        
        // System bookings this month
        val currentMonth = SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(Date())
        val systemThisMonthBookings = bookings.count { it.date.startsWith(currentMonth) }
        reportItems.add(ReportItem("System This Month", systemThisMonthBookings.toString(), R.drawable.ic_clock))
        
        // System recent bookings (last 7 days)
        val sevenDaysAgo = Calendar.getInstance().apply {
            add(Calendar.DAY_OF_MONTH, -7)
        }
        val recentSystemBookings = bookings.count { booking ->
            try {
                val bookingDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(booking.date)
                bookingDate?.after(sevenDaysAgo.time) ?: false
            } catch (e: Exception) {
                false
            }
        }
        reportItems.add(ReportItem("System Last 7 Days", recentSystemBookings.toString(), R.drawable.ic_edit))
        
        // System average bookings per day
        val avgSystemBookingsPerDay = if (bookings.isNotEmpty()) {
            val earliestDate = bookings.minByOrNull { it.date }?.date ?: currentMonth
            val parsedDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(earliestDate)
            val daysDiff = if (parsedDate != null) {
                (Date().time - parsedDate.time) / (1000 * 60 * 60 * 24)
            } else 0L
            if (daysDiff > 0) (bookings.size.toFloat() / daysDiff).toInt() else 0
        } else 0
        reportItems.add(ReportItem("System Avg/Day", avgSystemBookingsPerDay.toString(), R.drawable.ic_clock))
        
        // ========== USER-SPECIFIC ANALYTICS ==========
        
        // Current user info
        if (currentUser.isNotEmpty()) {
            reportItems.add(ReportItem("You are logged in as", currentUser, R.drawable.ic_user))
        }
        
        // User's total bookings
        reportItems.add(ReportItem("Your Total Bookings", userBookings.size.toString(), R.drawable.ic_calendar))
        
        // User's bookings this month
        val thisMonthUserBookings = userBookings.count { it.date.startsWith(currentMonth) }
        reportItems.add(ReportItem("Your This Month", thisMonthUserBookings.toString(), R.drawable.ic_clock))
        
        // User's most booked room
        if (userBookings.isNotEmpty()) {
            val userRoomBookingCounts = userBookings.groupingBy { it.roomName }.eachCount()
            val userMostBookedRoom = userRoomBookingCounts.maxByOrNull { it.value }
            if (userMostBookedRoom != null) {
                reportItems.add(ReportItem("Your Favorite Room", "${userMostBookedRoom.key} (${userMostBookedRoom.value})", R.drawable.ic_location))
            }
        }
        
        // User's recent bookings (last 7 days)
        val recentUserBookings = userBookings.count { booking ->
            try {
                val bookingDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(booking.date)
                bookingDate?.after(sevenDaysAgo.time) ?: false
            } catch (e: Exception) {
                false
            }
        }
        reportItems.add(ReportItem("Your Last 7 Days", recentUserBookings.toString(), R.drawable.ic_edit))
        
        // User's booking percentage of total system
        val userBookingPercentage = if (bookings.isNotEmpty()) {
            ((userBookings.size.toFloat() / bookings.size) * 100).toInt()
        } else 0
        reportItems.add(ReportItem("Your Share of Bookings", "$userBookingPercentage%", R.drawable.ic_user))
        
        return reportItems
    }
    
    private fun getCurrentUserInfo(): String {
        return try {
            val prefs = requireContext().getSharedPreferences("auth", Context.MODE_PRIVATE)
            val userJson = prefs.getString("user", null)
            if (userJson != null) {
                val user = JSONObject(userJson)
                val name = user.optString("name", "Unknown")
                val role = user.optString("role", "user")
                "$name ($role)"
            } else {
                "Not logged in"
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting current user info: ${e.message}")
            "Unknown User"
        }
    }
    
    private fun getCurrentUserId(): String {
        return try {
            val prefs = requireContext().getSharedPreferences("auth", Context.MODE_PRIVATE)
            val userJson = prefs.getString("user", null)
            if (userJson != null) {
                val user = JSONObject(userJson)
                user.optString("id", "")
            } else {
                ""
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting current user ID: ${e.message}")
            ""
        }
    }
    
    private fun setupCharts() {
        Log.d(TAG, "📊 Setting up charts")
        setupPieChart()
        setupBarChart()
        setupLineChart()
    }
    
    private fun setupPieChart() {
        val pieChart = binding.pieChartBookingStatus
        
        // Generate real data from bookings - show room type distribution
        val roomTypeCounts = allBookings.groupingBy { booking ->
            // Try to get room type from room data, fallback to room name
            val room = allRoomMap[booking.roomId]
            room?.roomType ?: booking.roomName
        }.eachCount()
        
        if (roomTypeCounts.isNotEmpty()) {
            val entries = roomTypeCounts.map { (roomType, count) ->
                PieEntry(count.toFloat(), roomType)
            }
            
            val dataSet = PieDataSet(entries, "Room Types (System-wide)")
            dataSet.colors = listOf(
                Color.parseColor("#87CEEB"), // Light Blue
                Color.parseColor("#5F9EA0"), // Darker Light Blue
                Color.parseColor("#4682B4"), // Steel Blue
                Color.parseColor("#B0E0E6"), // Lighter Light Blue
                Color.parseColor("#6495ED"), // Cornflower Blue
                Color.parseColor("#4169E1")  // Royal Blue
            )
            
            val data = PieData(dataSet)
            data.setValueTextSize(12f)
            data.setValueTextColor(Color.WHITE)
            
            pieChart.data = data
        } else {
            // Fallback data if no bookings
            val entries = listOf(PieEntry(1f, "No Bookings"))
            val dataSet = PieDataSet(entries, "Room Types")
            dataSet.colors = listOf(Color.parseColor("#CCCCCC"))
            val data = PieData(dataSet)
            pieChart.data = data
        }
        
        pieChart.description.isEnabled = false
        pieChart.legend.isEnabled = true
        pieChart.setHoleColor(Color.TRANSPARENT)
        pieChart.setTransparentCircleAlpha(0)
        pieChart.animateY(1000)
        pieChart.invalidate()
    }
    
    private fun setupBarChart() {
        val barChart = binding.barChartMonthly
        
        // Generate real monthly data from bookings
        val monthlyBookings = allBookings.groupBy { booking ->
            try {
                val date = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(booking.date)
                SimpleDateFormat("MMM", Locale.getDefault()).format(date ?: Date())
            } catch (e: Exception) {
                "Unknown"
            }
        }
        
        // Get last 6 months
        val calendar = Calendar.getInstance()
        val months = mutableListOf<String>()
        for (i in 5 downTo 0) {
            calendar.time = Date()
            calendar.add(Calendar.MONTH, -i)
            months.add(SimpleDateFormat("MMM", Locale.getDefault()).format(calendar.time))
        }
        
        val values = months.map { month ->
            monthlyBookings[month]?.size?.toFloat() ?: 0f
        }
        
        val entries = values.mapIndexed { index, value ->
            BarEntry(index.toFloat(), value)
        }
        
        val dataSet = BarDataSet(entries, "Monthly Bookings (System-wide)")
        dataSet.color = Color.parseColor("#87CEEB")
        dataSet.valueTextSize = 12f
        dataSet.valueTextColor = Color.BLACK
        
        val data = BarData(dataSet)
        barChart.data = data
        
        val xAxis = barChart.xAxis
        xAxis.position = XAxis.XAxisPosition.BOTTOM
        xAxis.setDrawGridLines(false)
        xAxis.valueFormatter = IndexAxisValueFormatter(months)
        xAxis.textSize = 10f
        
        barChart.description.isEnabled = false
        barChart.legend.isEnabled = false
        barChart.animateY(1000)
        barChart.invalidate()
    }
    
    private fun setupLineChart() {
        val lineChart = binding.lineChartRevenue
        
        // Sample revenue data
        val entries = listOf(
            Entry(0f, 45000f),
            Entry(1f, 52000f),
            Entry(2f, 48000f),
            Entry(3f, 61000f),
            Entry(4f, 55000f),
            Entry(5f, 67000f)
        )
        
        val dataSet = LineDataSet(entries, "Revenue (KSh)")
        dataSet.color = Color.parseColor("#5F9EA0")
        dataSet.valueTextSize = 12f
        dataSet.valueTextColor = Color.BLACK
        dataSet.lineWidth = 3f
        dataSet.circleRadius = 5f
        dataSet.setCircleColor(Color.parseColor("#5F9EA0"))
        
        val data = LineData(dataSet)
        lineChart.data = data
        
        val xAxis = lineChart.xAxis
        xAxis.position = XAxis.XAxisPosition.BOTTOM
        xAxis.setDrawGridLines(false)
        xAxis.valueFormatter = object : ValueFormatter() {
            override fun getFormattedValue(value: Float): String {
                return when (value.toInt()) {
                    0 -> "Jan"
                    1 -> "Feb"
                    2 -> "Mar"
                    3 -> "Apr"
                    4 -> "May"
                    5 -> "Jun"
                    else -> ""
                }
            }
        }
        
        lineChart.description.isEnabled = false
        lineChart.legend.isEnabled = false
        lineChart.animateY(1000)
        lineChart.invalidate()
    }

    private fun setupFabClickListener() {
        // PDF Export FAB (Left)
        binding.fabExportPdf.setOnClickListener {
            checkStoragePermissionAndExportPdf()
        }
        
        // More Actions FAB (Right) - AI Assistant
        binding.fabMoreActions.setOnClickListener {
            showAIAssistant()
        }
    }
    
    private fun showAIAssistant() {
        val aiDialog = AIAssistantDialog()
        aiDialog.show(parentFragmentManager, "AIAssistantDialog")
    }
    

    private fun checkStoragePermissionAndExportPdf() {
        val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.READ_MEDIA_IMAGES
        } else {
            Manifest.permission.WRITE_EXTERNAL_STORAGE
        }

        if (ContextCompat.checkSelfPermission(requireContext(), permission) == PackageManager.PERMISSION_GRANTED) {
            exportReportToPdf()
        } else {
            ActivityCompat.requestPermissions(requireActivity(), arrayOf(permission), STORAGE_PERMISSION_CODE)
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == STORAGE_PERMISSION_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                exportReportToPdf()
            } else {
                Toast.makeText(requireContext(), "Storage permission denied. Cannot export PDF.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun exportReportToPdf() {
        lifecycleScope.launch {
            try {
                binding.loadingProgressBar.visibility = View.VISIBLE
                
                val reportData = generateReportData(allBookings)
                val fileName = "MosBookings_Report_${SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())}.pdf"
                val file = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), fileName)
                
                val pdfWriter = PdfWriter(FileOutputStream(file))
                val pdfDocument = PdfDocument(pdfWriter)
                val document = Document(pdfDocument)
                
                // Add title
                val title = Paragraph("MosBookings - Reports & Analytics")
                    .setTextAlignment(TextAlignment.CENTER)
                    .setFontSize(20f)
                    .setBold()
                    .setMarginBottom(20f)
                document.add(title)
                
                // Add generation date
                val date = Paragraph("Generated on: ${SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date())}")
                    .setTextAlignment(TextAlignment.CENTER)
                    .setFontSize(12f)
                    .setMarginBottom(30f)
                document.add(date)
                
                // Add user info
                val currentUser = getCurrentUserInfo()
                if (currentUser.isNotEmpty()) {
                    val userInfo = Paragraph("Report for: $currentUser")
                        .setFontSize(14f)
                        .setMarginBottom(20f)
                    document.add(userInfo)
                }
                
                // Add summary statistics
                val summaryTitle = Paragraph("Summary Statistics")
                    .setFontSize(16f)
                    .setBold()
                    .setMarginBottom(15f)
                document.add(summaryTitle)
                
                // Create summary table
                val summaryTable = Table(UnitValue.createPercentArray(floatArrayOf(60f, 40f)))
                    .setWidth(UnitValue.createPercentValue(100f))
                    .setMarginBottom(20f)
                
                // Add table headers
                summaryTable.addHeaderCell(Cell().add(Paragraph("Metric").setBold()))
                summaryTable.addHeaderCell(Cell().add(Paragraph("Value").setBold()))
                
                // Add summary data
                val summaryItems = listOf(
                    "Total System Bookings" to allBookings.size.toString(),
                    "Your Total Bookings" to allBookings.filter { it.user == getCurrentUserId() }.size.toString(),
                    "System This Month" to allBookings.count { it.date.startsWith(SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(Date())) }.toString(),
                    "Your This Month" to allBookings.filter { it.user == getCurrentUserId() }.count { it.date.startsWith(SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(Date())) }.toString()
                )
                
                summaryItems.forEach { (metric, value) ->
                    summaryTable.addCell(Cell().add(Paragraph(metric)))
                    summaryTable.addCell(Cell().add(Paragraph(value)))
                }
                
                document.add(summaryTable)
                
                // Add detailed reports
                val detailsTitle = Paragraph("Detailed Reports")
                    .setFontSize(16f)
                    .setBold()
                    .setMarginBottom(15f)
                document.add(detailsTitle)
                
                // Create detailed reports table
                val detailsTable = Table(UnitValue.createPercentArray(floatArrayOf(50f, 50f)))
                    .setWidth(UnitValue.createPercentValue(100f))
                    .setMarginBottom(20f)
                
                // Add table headers
                detailsTable.addHeaderCell(Cell().add(Paragraph("Report Item").setBold()))
                detailsTable.addHeaderCell(Cell().add(Paragraph("Value").setBold()))
                
                // Add report data
                reportData.forEach { item ->
                    detailsTable.addCell(Cell().add(Paragraph(item.title)))
                    detailsTable.addCell(Cell().add(Paragraph(item.value)))
                }
                
                document.add(detailsTable)
                
                // Add room type distribution
                if (allBookings.isNotEmpty()) {
                    val roomTypeTitle = Paragraph("Room Type Distribution")
                        .setFontSize(16f)
                        .setBold()
                        .setMarginBottom(15f)
                    document.add(roomTypeTitle)
                    
                    val roomTypeCounts = allBookings.groupingBy { booking ->
                        val room = allRoomMap[booking.roomId]
                        room?.roomType ?: booking.roomName
                    }.eachCount()
                    
                    val roomTypeTable = Table(UnitValue.createPercentArray(floatArrayOf(60f, 40f)))
                        .setWidth(UnitValue.createPercentValue(100f))
                        .setMarginBottom(20f)
                    
                    roomTypeTable.addHeaderCell(Cell().add(Paragraph("Room Type").setBold()))
                    roomTypeTable.addHeaderCell(Cell().add(Paragraph("Bookings").setBold()))
                    
                    roomTypeCounts.forEach { (roomType, count) ->
                        roomTypeTable.addCell(Cell().add(Paragraph(roomType)))
                        roomTypeTable.addCell(Cell().add(Paragraph(count.toString())))
                    }
                    
                    document.add(roomTypeTable)
                }
                
                // Add footer
                val footer = Paragraph("This report was generated by MosBookings App")
                    .setTextAlignment(TextAlignment.CENTER)
                    .setFontSize(10f)
                    .setMarginTop(30f)
                document.add(footer)
                
                document.close()
                
                if (isAdded && view != null) {
                    binding.loadingProgressBar.visibility = View.GONE
                    Toast.makeText(requireContext(), "Report exported successfully to Downloads folder: $fileName", Toast.LENGTH_LONG).show()
                }
                
            } catch (e: Exception) {
                Log.e(TAG, "Error exporting PDF: ${e.message}")
                if (isAdded && view != null) {
                    binding.loadingProgressBar.visibility = View.GONE
                    Toast.makeText(requireContext(), "Error exporting PDF: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun showContent() {
        binding.reportsRecyclerView.visibility = View.VISIBLE
        val emptyState = binding.root.findViewById<View>(R.id.emptyState)
        emptyState.visibility = View.GONE
    }

    private fun showEmptyState(title: String, message: String) {
        binding.reportsRecyclerView.visibility = View.GONE
        val emptyState = binding.root.findViewById<View>(R.id.emptyState)
        emptyState.visibility = View.VISIBLE
        
        // Get references to empty state views
        val emptyStateTitle = binding.root.findViewById<android.widget.TextView>(R.id.emptyStateTitle)
        val emptyStateMessage = binding.root.findViewById<android.widget.TextView>(R.id.emptyStateMessage)
        val emptyStateAction = binding.root.findViewById<android.widget.Button>(R.id.emptyStateAction)
        
        // Update empty state text
        emptyStateTitle.text = title
        emptyStateMessage.text = message
        emptyStateAction.text = "Refresh"
        emptyStateAction.visibility = View.VISIBLE
        
        // Set refresh action
        emptyStateAction.setOnClickListener {
            loadReports()
        }
    }

    data class ReportItem(
        val title: String,
        val value: String,
        val iconRes: Int
    )
}
