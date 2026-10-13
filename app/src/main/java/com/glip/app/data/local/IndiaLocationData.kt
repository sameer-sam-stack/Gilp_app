package com.glip.app.data.local

data class CityStateItem(
    val city: String,
    val state: String
)

object IndiaLocationData {

    val statesAndCities: Map<String, List<String>> = linkedMapOf(
        "Chhattisgarh" to listOf(
            "Rajnandgaon", "Raipur", "Bhilai", "Bilaspur", "Durg",
            "Korba", "Jagdalpur", "Ambikapur", "Raigarh", "Dhamtari",
            "Mahasamund", "Kawardha", "Kanker", "Champa", "Bhatapara"
        ),
        "Delhi" to listOf(
            "New Delhi", "Connaught Place", "Saket", "Karol Bagh", "Dwarka",
            "Rohini", "Hauz Khas", "Lajpat Nagar", "Janakpuri", "Chandni Chowk",
            "Vasant Kunj", "Mayur Vihar", "Pitampura", "Nehru Place"
        ),
        "Maharashtra" to listOf(
            "Mumbai", "Pune", "Nagpur", "Thane", "Nashik",
            "Navi Mumbai", "Aurangabad", "Solapur", "Kolhapur", "Amravati",
            "Nanded", "Jalgaon", "Akola", "Panvel", "Kalyan", "Vasai-Virar"
        ),
        "Karnataka" to listOf(
            "Bengaluru", "Mysuru", "Hubballi", "Dharwad", "Mangaluru",
            "Belagavi", "Davanagere", "Ballari", "Vijayapura", "Shivamogga",
            "Tumakuru", "Kalaburagi", "Udupi", "Hassan", "Bidar"
        ),
        "Haryana" to listOf(
            "Gurugram", "Faridabad", "Panipat", "Ambala", "Yamunanagar",
            "Rohtak", "Hisar", "Karnal", "Sonipat", "Panchkula",
            "Bhiwani", "Sirsa", "Bahadurgarh", "Jind", "Thanesar"
        ),
        "Uttar Pradesh" to listOf(
            "Noida", "Greater Noida", "Lucknow", "Kanpur", "Ghaziabad",
            "Agra", "Varanasi", "Prayagraj", "Meerut", "Bareilly",
            "Aligarh", "Moradabad", "Gorakhpur", "Jhansi", "Mathura", "Ayodhya"
        ),
        "Gujarat" to listOf(
            "Ahmedabad", "Surat", "Vadodara", "Rajkot", "Bhavnagar",
            "Jamnagar", "Gandhinagar", "Junagadh", "Anand", "Navsari",
            "Morbi", "Bharuch", "Porbandar", "Mehsana", "Vapi"
        ),
        "Madhya Pradesh" to listOf(
            "Indore", "Bhopal", "Jabalpur", "Gwalior", "Ujjain",
            "Sagar", "Dewas", "Satna", "Ratlam", "Rewa",
            "Singrauli", "Burhanpur", "Khandwa", "Chhindwara", "Shivpuri"
        ),
        "Rajasthan" to listOf(
            "Jaipur", "Jodhpur", "Kota", "Bikaner", "Ajmer",
            "Udaipur", "Bhilwara", "Alwar", "Bharatpur", "Sikar",
            "Pali", "Sri Ganganagar", "Barmer", "Chittorgarh", "Beawar"
        ),
        "Tamil Nadu" to listOf(
            "Chennai", "Coimbatore", "Madurai", "Tiruchirappalli", "Salem",
            "Tiruppur", "Erode", "Tirunelveli", "Vellore", "Thoothukudi",
            "Dindigul", "Thanjavur", "Ranipet", "Sivakasi", "Karur"
        ),
        "Telangana" to listOf(
            "Hyderabad", "Warangal", "Nizamabad", "Khammam", "Karimnagar",
            "Ramagundam", "Mahbubnagar", "Nalgonda", "Adilabad", "Suryapet"
        ),
        "West Bengal" to listOf(
            "Kolkata", "Howrah", "Siliguri", "Asansol", "Durgapur",
            "Bardhaman", "Malda", "Baharampur", "Habra", "Kharagpur", "Shantipur"
        ),
        "Bihar" to listOf(
            "Patna", "Gaya", "Bhagalpur", "Muzaffarpur", "Purnia",
            "Darbhanga", "Bihar Sharif", "Arrah", "Begusarai", "Katihar", "Munger"
        ),
        "Punjab" to listOf(
            "Ludhiana", "Amritsar", "Jalandhar", "Patiala", "Bathinda",
            "Mohali", "Hoshiarpur", "Batala", "Pathankot", "Moga", "Abohar"
        ),
        "Kerala" to listOf(
            "Kochi", "Thiruvananthapuram", "Kozhikode", "Thrissur", "Kollam",
            "Alappuzha", "Palakkad", "Malappuram", "Kannur", "Kottayam"
        ),
        "Andhra Pradesh" to listOf(
            "Visakhapatnam", "Vijayawada", "Guntur", "Nellore", "Kurnool",
            "Rajahmundry", "Tirupati", "Kakinada", "Kadapa", "Anantapur"
        ),
        "Odisha" to listOf(
            "Bhubaneswar", "Cuttack", "Rourkela", "Berhampur", "Sambalpur",
            "Puri", "Balasore", "Bhadrak", "Baripada", "Jharsuguda"
        ),
        "Jharkhand" to listOf(
            "Ranchi", "Jamshedpur", "Dhanbad", "Bokaro", "Deoghar",
            "Hazaribagh", "Giridih", "Ramgarh", "Medininagar", "Chirkunda"
        ),
        "Assam" to listOf(
            "Guwahati", "Silchar", "Dibrugarh", "Jorhat", "Nagaon",
            "Tinsukia", "Tezpur", "Bongaigaon", "Karimganj", "Sivasagar"
        ),
        "Uttarakhand" to listOf(
            "Dehradun", "Haridwar", "Roorkee", "Haldwani", "Rudrapur",
            "Kashipur", "Rishikesh", "Nainital", "Pithoragarh", "Kotdwar"
        ),
        "Himachal Pradesh" to listOf(
            "Shimla", "Dharamshala", "Solan", "Mandi", "Kullu",
            "Manali", "Baddi", "Bilaspur", "Hamirpur", "Una"
        ),
        "Goa" to listOf(
            "Panaji", "Margao", "Vasco da Gama", "Mapusa", "Ponda"
        ),
        "Jammu and Kashmir" to listOf(
            "Srinagar", "Jammu", "Anantnag", "Baramulla", "Kathua", "Udhampur", "Sopore"
        ),
        "Chandigarh" to listOf("Chandigarh"),
        "Tripura" to listOf("Agartala", "Dharmanagar", "Udaipur", "Kailashahar"),
        "Manipur" to listOf("Imphal", "Churachandpur", "Thoubal", "Bishnupur"),
        "Meghalaya" to listOf("Shillong", "Tura", "Jowai", "Nongpoh"),
        "Nagaland" to listOf("Kohima", "Dimapur", "Mokokchung", "Tuensang"),
        "Mizoram" to listOf("Aizawl", "Lunglei", "Champhai", "Serchhip"),
        "Arunachal Pradesh" to listOf("Itanagar", "Naharlagun", "Pasighat", "Tawang"),
        "Sikkim" to listOf("Gangtok", "Namchi", "Geyzing", "Mangan"),
        "Puducherry" to listOf("Puducherry", "Karaikal", "Mahe", "Yanam"),
        "Ladakh" to listOf("Leh", "Kargil"),
        "Andaman and Nicobar" to listOf("Port Blair"),
        "Dadra and Nagar Haveli and Daman and Diu" to listOf("Daman", "Diu", "Silvassa"),
        "Lakshadweep" to listOf("Kavaratti")
    )

    fun getAllStates(): List<String> = statesAndCities.keys.toList()

    fun getCitiesForState(state: String): List<String> = statesAndCities[state] ?: emptyList()

    fun search(query: String): List<CityStateItem> {
        val clean = query.trim().lowercase()
        if (clean.isBlank()) {
            // Return top major cities by default
            return statesAndCities.flatMap { (state, cities) ->
                cities.take(2).map { CityStateItem(it, state) }
            }
        }

        val results = mutableListOf<CityStateItem>()

        // 1. Exact or prefix matches for cities
        statesAndCities.forEach { (state, cities) ->
            cities.forEach { city ->
                if (city.lowercase().contains(clean)) {
                    results.add(CityStateItem(city, state))
                }
            }
        }

        // 2. State matches (if query matches a state, list cities from that state)
        if (results.size < 15) {
            statesAndCities.forEach { (state, cities) ->
                if (state.lowercase().contains(clean)) {
                    cities.forEach { city ->
                        val item = CityStateItem(city, state)
                        if (!results.contains(item)) {
                            results.add(item)
                        }
                    }
                }
            }
        }

        return results.take(40)
    }
}
