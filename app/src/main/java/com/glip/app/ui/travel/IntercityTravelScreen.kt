package com.glip.app.ui.travel

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.glip.app.domain.model.UserProfile
import com.glip.app.ui.theme.GlipBrandYellow

data class BusItem(
    val id: String,
    val operatorName: String,
    val departureTime: String,
    val arrivalTime: String,
    val duration: String,
    val busType: String,
    val price: Int,
    val rating: Double,
    val reviewsCount: Int,
    val seatsAvailable: Int
)

val SAMPLE_BUSES = listOf(
    BusItem("1", "City Boom", "10:20 AM", "12:00 PM", "1h 40m", "2+2 AC Seater", 220, 4.6, 128, 18),
    BusItem("2", "Roadline", "11:22 AM", "02:00 PM", "2h 38m", "AC Sleeper 2+1", 280, 4.8, 340, 8),
    BusItem("3", "Payal Travels", "01:15 PM", "03:45 PM", "2h 30m", "Non-AC Seater 2+2", 210, 4.3, 89, 24),
    BusItem("4", "Kanker Roadways", "03:30 PM", "06:10 PM", "2h 40m", "BharatBenz AC Seater", 320, 4.9, 512, 5),
    BusItem("5", "Royal Travels CG", "05:00 PM", "07:30 PM", "2h 30m", "AC Sleeper / Seater", 290, 4.7, 215, 14),
    BusItem("6", "Mahendra Travels", "07:45 PM", "10:15 PM", "2h 30m", "Multi-Axle Volvo AC", 350, 4.8, 420, 12),
    BusItem("7", "Dubey Travels", "09:15 PM", "11:45 PM", "2h 30m", "2+2 Air Suspension", 240, 4.4, 167, 22),
    BusItem("8", "Bastariya Express", "10:30 PM", "01:15 AM", "2h 45m", "Premium AC Sleeper", 310, 4.7, 198, 9)
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IntercityTravelScreen(
    userProfile: UserProfile?,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val defaultName = userProfile?.fullName?.ifBlank { "Sameer" } ?: "Sameer"
    val defaultPhone = userProfile?.phoneNumber?.ifBlank { "9752304981" } ?: "9752304981"

    var pickupCity by remember { mutableStateOf("Rajnandgaon") }
    var dropCity by remember { mutableStateOf("Raipur") }

    var isSelectingPickupCity by remember { mutableStateOf(false) }
    var isSelectingDropCity by remember { mutableStateOf(false) }

    var hasSearched by remember { mutableStateOf(false) }
    var selectedBus by remember { mutableStateOf<BusItem?>(null) }
    var showBookingForm by remember { mutableStateOf(false) }

    // Booking form inputs
    var userNameInput by remember { mutableStateOf(defaultName) }
    var userContactInput by remember { mutableStateOf(defaultPhone) }
    var isBookingForSomeone by remember { mutableStateOf(false) }
    var otherPersonName by remember { mutableStateOf("") }
    var otherPersonContact by remember { mutableStateOf("") }

    // Confirmation Dialog
    var showConfirmDialog by remember { mutableStateOf(false) }
    var confirmedBus by remember { mutableStateOf<BusItem?>(null) }

    BackHandler {
        if (showBookingForm) {
            showBookingForm = false
        } else {
            onNavigateBack()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = if (showBookingForm) "Passenger Details" else "Intercity Travels",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0F172A)
                        )
                        Text(
                            text = if (showBookingForm) "$pickupCity to $dropCity" else "Chhattisgarh Bus Booking",
                            fontSize = 12.sp,
                            color = Color(0xFF64748B)
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = {
                        if (showBookingForm) {
                            showBookingForm = false
                        } else {
                            onNavigateBack()
                        }
                    }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color(0xFF0F172A)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.White
                )
            )
        },
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(Color(0xFFF8FAFC))
        ) {
            if (showBookingForm && selectedBus != null) {
                // ==========================================
                // SCREEN 2: BUS BOOKING FORM
                // ==========================================
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                ) {
                    // Selected Bus summary card
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(2.dp),
                        border = BorderStroke(1.dp, Color(0xFFE2E8F0))
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = selectedBus!!.operatorName,
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF0F172A)
                                )
                                Text(
                                    text = "₹${selectedBus!!.price}",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF15803D)
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "${selectedBus!!.departureTime} ($pickupCity)",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = Color(0xFF334155)
                                )
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                    contentDescription = null,
                                    tint = Color(0xFF94A3B8),
                                    modifier = Modifier
                                        .padding(horizontal = 6.dp)
                                        .size(14.dp)
                                )
                                Text(
                                    text = "${selectedBus!!.arrivalTime} ($dropCity)",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = Color(0xFF334155)
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = selectedBus!!.busType,
                                fontSize = 12.sp,
                                color = Color(0xFF64748B)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Form card
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(2.dp),
                        border = BorderStroke(1.dp, Color(0xFFE2E8F0))
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "Your Details",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF0F172A)
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            OutlinedTextField(
                                value = userNameInput,
                                onValueChange = { userNameInput = it },
                                label = { Text("Name") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp)
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            OutlinedTextField(
                                value = userContactInput,
                                onValueChange = { userContactInput = it },
                                label = { Text("Contact Number") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp)
                            )

                            Spacer(modifier = Modifier.height(14.dp))

                            // Check Box: Booking for someone else
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { isBookingForSomeone = !isBookingForSomeone }
                                    .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(
                                    checked = isBookingForSomeone,
                                    onCheckedChange = { isBookingForSomeone = it },
                                    colors = CheckboxDefaults.colors(checkedColor = Color(0xFF0F172A))
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Booking for someone else",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = Color(0xFF0F172A)
                                )
                            }

                            // If Booking for someone else is checked
                            if (isBookingForSomeone) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(Color(0xFFF8FAFC), RoundedCornerShape(12.dp))
                                        .padding(12.dp)
                                ) {
                                    Text(
                                        text = "Passenger's Details",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF0284C7)
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    OutlinedTextField(
                                        value = otherPersonName,
                                        onValueChange = { otherPersonName = it },
                                        label = { Text("Passenger Name") },
                                        singleLine = true,
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(10.dp)
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    OutlinedTextField(
                                        value = otherPersonContact,
                                        onValueChange = { otherPersonContact = it },
                                        label = { Text("Passenger Contact Number") },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                                        singleLine = true,
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(10.dp)
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.weight(1f))

                    // Book Button
                    Button(
                        onClick = {
                            confirmedBus = selectedBus
                            showConfirmDialog = true
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp),
                        shape = RoundedCornerShape(27.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = GlipBrandYellow,
                            contentColor = Color(0xFF0F172A)
                        ),
                        elevation = ButtonDefaults.buttonElevation(2.dp)
                    ) {
                        Text(
                            text = "Book Ticket • ₹${selectedBus!!.price}",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0F172A)
                        )
                    }
                }
            } else {
                // ==========================================
                // SCREEN 1: SEARCH & BUS LIST
                // ==========================================
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Search Card
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(18.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            elevation = CardDefaults.cardElevation(2.dp),
                            border = BorderStroke(1.dp, Color(0xFFE2E8F0))
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    text = "Select Route",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF0F172A)
                                )

                                Spacer(modifier = Modifier.height(14.dp))

                                // Pickup City selector
                                Surface(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { isSelectingPickupCity = true },
                                    shape = RoundedCornerShape(12.dp),
                                    color = Color(0xFFF8FAFC),
                                    border = BorderStroke(1.dp, Color(0xFFE2E8F0))
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.LocationOn,
                                            contentDescription = null,
                                            tint = Color(0xFF15803D),
                                            modifier = Modifier.size(22.dp)
                                        )
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = "Pickup City",
                                                fontSize = 11.sp,
                                                color = Color(0xFF64748B)
                                            )
                                            Text(
                                                text = pickupCity,
                                                fontSize = 15.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFF0F172A)
                                            )
                                        }
                                        Icon(
                                            imageVector = Icons.Default.ArrowDropDown,
                                            contentDescription = null,
                                            tint = Color(0xFF64748B)
                                        )
                                    }
                                }

                                // Switch Button Row
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp),
                                    contentAlignment = Alignment.CenterEnd
                                ) {
                                    Surface(
                                        shape = CircleShape,
                                        color = Color.White,
                                        border = BorderStroke(1.dp, Color(0xFFCBD5E1)),
                                        shadowElevation = 2.dp,
                                        modifier = Modifier.clickable {
                                            val temp = pickupCity
                                            pickupCity = dropCity
                                            dropCity = temp
                                        }
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.SwapVert,
                                            contentDescription = "Switch Cities",
                                            tint = Color(0xFF0F172A),
                                            modifier = Modifier
                                                .padding(8.dp)
                                                .size(20.dp)
                                        )
                                    }
                                }

                                // Drop City selector
                                Surface(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { isSelectingDropCity = true },
                                    shape = RoundedCornerShape(12.dp),
                                    color = Color(0xFFF8FAFC),
                                    border = BorderStroke(1.dp, Color(0xFFE2E8F0))
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.PinDrop,
                                            contentDescription = null,
                                            tint = Color(0xFFDC2626),
                                            modifier = Modifier.size(22.dp)
                                        )
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = "Drop City",
                                                fontSize = 11.sp,
                                                color = Color(0xFF64748B)
                                            )
                                            Text(
                                                text = dropCity,
                                                fontSize = 15.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFF0F172A)
                                            )
                                        }
                                        Icon(
                                            imageVector = Icons.Default.ArrowDropDown,
                                            contentDescription = null,
                                            tint = Color(0xFF64748B)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(18.dp))

                                // Search Button
                                Button(
                                    onClick = {
                                        hasSearched = true
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(50.dp),
                                    shape = RoundedCornerShape(25.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = GlipBrandYellow,
                                        contentColor = Color(0xFF0F172A)
                                    ),
                                    elevation = ButtonDefaults.buttonElevation(2.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Search,
                                        contentDescription = "Search",
                                        tint = Color(0xFF0F172A),
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Search Buses",
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF0F172A)
                                    )
                                }
                            }
                        }
                    }

                    // Available Buses Header
                    if (hasSearched) {
                        item {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 8.dp, bottom = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Available Buses (${SAMPLE_BUSES.size})",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF0F172A)
                                )
                                Text(
                                    text = "$pickupCity → $dropCity",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = Color(0xFF64748B)
                                )
                            }
                        }

                        // Bus Items
                        items(SAMPLE_BUSES) { bus ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        selectedBus = bus
                                        showBookingForm = true
                                    },
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = Color.White),
                                elevation = CardDefaults.cardElevation(2.dp),
                                border = BorderStroke(1.dp, Color(0xFFE2E8F0))
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    // Row 1: Operator Name + Price
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column {
                                            Text(
                                                text = bus.operatorName,
                                                fontSize = 16.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFF0F172A)
                                            )
                                            Text(
                                                text = bus.busType,
                                                fontSize = 12.sp,
                                                color = Color(0xFF64748B)
                                            )
                                        }
                                        Column(horizontalAlignment = Alignment.End) {
                                            Text(
                                                text = "₹${bus.price}",
                                                fontSize = 18.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFF0F172A)
                                            )
                                            Text(
                                                text = "per seat",
                                                fontSize = 11.sp,
                                                color = Color(0xFF94A3B8)
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(12.dp))

                                    // Row 2: Timings & Route
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = bus.departureTime,
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF0F172A)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .height(1.dp)
                                                .background(Color(0xFFCBD5E1))
                                        )
                                        Text(
                                            text = " ${bus.duration} ",
                                            fontSize = 11.sp,
                                            color = Color(0xFF64748B)
                                        )
                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .height(1.dp)
                                                .background(Color(0xFFCBD5E1))
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = bus.arrivalTime,
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF0F172A)
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(10.dp))
                                    HorizontalDivider(color = Color(0xFFF1F5F9))
                                    Spacer(modifier = Modifier.height(10.dp))

                                    // Row 3: Rating, Seats & Select CTA
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Surface(
                                                shape = RoundedCornerShape(6.dp),
                                                color = Color(0xFF15803D)
                                            ) {
                                                Row(
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.Star,
                                                        contentDescription = null,
                                                        tint = Color.White,
                                                        modifier = Modifier.size(12.dp)
                                                    )
                                                    Spacer(modifier = Modifier.width(3.dp))
                                                    Text(
                                                        text = "${bus.rating}",
                                                        fontSize = 11.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = Color.White
                                                    )
                                                }
                                            }
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = "${bus.seatsAvailable} seats left",
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Medium,
                                                color = if (bus.seatsAvailable <= 8) Color(0xFFDC2626) else Color(0xFF15803D)
                                            )
                                        }

                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                text = "Select Bus",
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFF0284C7)
                                            )
                                            Icon(
                                                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                                contentDescription = null,
                                                tint = Color(0xFF0284C7),
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // City Selection Bottom Sheets
    if (isSelectingPickupCity) {
        CitySelectionBottomSheet(
            title = "Select Pickup City",
            selectedCity = pickupCity,
            onCitySelected = { pickupCity = it },
            onDismiss = { isSelectingPickupCity = false }
        )
    }

    if (isSelectingDropCity) {
        CitySelectionBottomSheet(
            title = "Select Drop City",
            selectedCity = dropCity,
            onCitySelected = { dropCity = it },
            onDismiss = { isSelectingDropCity = false }
        )
    }

    // Confirmation Pop-up Dialog
    if (showConfirmDialog && confirmedBus != null) {
        val passenger = if (isBookingForSomeone && otherPersonName.isNotBlank()) {
            "$otherPersonName ($otherPersonContact)"
        } else {
            "$userNameInput ($userContactInput)"
        }

        AlertDialog(
            onDismissRequest = {
                showConfirmDialog = false
                showBookingForm = false
            },
            icon = {
                Box(
                    modifier = Modifier
                        .size(54.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFDCFCE7)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = "Confirmed",
                        tint = Color(0xFF15803D),
                        modifier = Modifier.size(34.dp)
                    )
                }
            },
            title = {
                Text(
                    text = "Booking Confirmed! 🎉",
                    fontWeight = FontWeight.Bold,
                    fontSize = 19.sp,
                    color = Color(0xFF0F172A)
                )
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Your bus seat on ${confirmedBus!!.operatorName} has been booked successfully.",
                        fontSize = 14.sp,
                        color = Color(0xFF475569)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFFF8FAFC),
                        border = BorderStroke(1.dp, Color(0xFFE2E8F0))
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(text = "Route:", fontSize = 12.sp, color = Color(0xFF64748B))
                                Text(
                                    text = "$pickupCity → $dropCity",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF0F172A)
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(text = "Departure:", fontSize = 12.sp, color = Color(0xFF64748B))
                                Text(
                                    text = confirmedBus!!.departureTime,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF0F172A)
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(text = "Passenger:", fontSize = 12.sp, color = Color(0xFF64748B))
                                Text(
                                    text = passenger,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF0F172A)
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(text = "Fare:", fontSize = 12.sp, color = Color(0xFF64748B))
                                Text(
                                    text = "₹${confirmedBus!!.price}",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF15803D)
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showConfirmDialog = false
                        showBookingForm = false
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = GlipBrandYellow,
                        contentColor = Color(0xFF0F172A)
                    ),
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Done",
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0F172A)
                    )
                }
            }
        )
    }
}
