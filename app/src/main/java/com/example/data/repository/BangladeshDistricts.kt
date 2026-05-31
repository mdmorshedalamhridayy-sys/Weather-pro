package com.example.data.repository

data class PresetLocation(
    val name: String,
    val latitude: Double,
    val longitude: Double,
    val isInternational: Boolean = false,
    val division: String = ""
)

object PresetLocations {
    val districts = listOf(
        // Bangladesh Districts
        PresetLocation("Dhaka", 23.8103, 90.4125, division = "Dhaka"),
        PresetLocation("Gazipur", 24.0023, 90.4267, division = "Dhaka"),
        PresetLocation("Narayanganj", 23.6238, 90.5000, division = "Dhaka"),
        PresetLocation("Tangail", 24.2513, 89.9167, division = "Dhaka"),
        PresetLocation("Faridpur", 23.6071, 89.8429, division = "Dhaka"),
        PresetLocation("Gopalganj", 23.0074, 89.8273, division = "Dhaka"),
        PresetLocation("Kishoreganj", 24.4375, 90.7816, division = "Dhaka"),
        PresetLocation("Madaripur", 23.1641, 90.1896, division = "Dhaka"),
        PresetLocation("Manikganj", 23.8644, 90.0047, division = "Dhaka"),
        PresetLocation("Munshiganj", 23.5435, 90.5361, division = "Dhaka"),
        PresetLocation("Narsingdi", 23.9229, 90.7171, division = "Dhaka"),
        PresetLocation("Rajbari", 23.7574, 89.6444, division = "Dhaka"),
        PresetLocation("Shariatpur", 23.2160, 90.3547, division = "Dhaka"),

        PresetLocation("Chattogram", 22.3569, 91.7832, division = "Chattogram"),
        PresetLocation("Cox's Bazar", 21.4272, 92.0058, division = "Chattogram"),
        PresetLocation("Comilla", 23.4682, 91.1786, division = "Chattogram"),
        PresetLocation("Feni", 23.0159, 91.3976, division = "Chattogram"),
        PresetLocation("Brahmanbaria", 23.9571, 91.1119, division = "Chattogram"),
        PresetLocation("Chandpur", 23.2333, 90.6500, division = "Chattogram"),
        PresetLocation("Lakshmipur", 22.9426, 90.8417, division = "Chattogram"),
        PresetLocation("Noakhali", 22.8246, 91.1017, division = "Chattogram"),
        PresetLocation("Bandarban", 22.1953, 92.2184, division = "Chattogram"),
        PresetLocation("Khagrachhari", 23.1115, 91.9995, division = "Chattogram"),
        PresetLocation("Rangamati", 22.6516, 92.1795, division = "Chattogram"),

        PresetLocation("Sylhet", 24.8949, 91.8687, division = "Sylhet"),
        PresetLocation("Moulvibazar", 24.4820, 91.7685, division = "Sylhet"),
        PresetLocation("Habiganj", 24.3749, 91.4132, division = "Sylhet"),
        PresetLocation("Sunamganj", 25.0658, 91.4058, division = "Sylhet"),

        PresetLocation("Khulna", 22.8456, 89.5403, division = "Khulna"),
        PresetLocation("Bagerhat", 22.6516, 89.7859, division = "Khulna"),
        PresetLocation("Satkhira", 22.7185, 89.0705, division = "Khulna"),
        PresetLocation("Jessore", 23.1697, 89.2137, division = "Khulna"),
        PresetLocation("Magura", 23.4873, 89.4199, division = "Khulna"),
        PresetLocation("Narail", 23.1683, 89.5001, division = "Khulna"),
        PresetLocation("Kushtia", 23.9013, 89.1204, division = "Khulna"),
        PresetLocation("Meherpur", 23.7622, 88.6318, division = "Khulna"),
        PresetLocation("Chuadanga", 23.6421, 88.8543, division = "Khulna"),
        PresetLocation("Jhenaidah", 23.5450, 89.1726, division = "Khulna"),

        PresetLocation("Rajshahi", 24.3636, 88.6241, division = "Rajshahi"),
        PresetLocation("Bogra", 24.8481, 89.3730, division = "Rajshahi"),
        PresetLocation("Joypurhat", 25.0968, 89.0227, division = "Rajshahi"),
        PresetLocation("Naogaon", 24.8115, 88.9481, division = "Rajshahi"),
        PresetLocation("Natore", 24.4102, 88.9546, division = "Rajshahi"),
        PresetLocation("Nawabganj", 24.5960, 88.2711, division = "Rajshahi"),
        PresetLocation("Pabna", 24.0042, 89.2444, division = "Rajshahi"),
        PresetLocation("Sirajganj", 24.4577, 89.7080, division = "Rajshahi"),

        PresetLocation("Barisal", 22.7010, 90.3535, division = "Barisal"),
        PresetLocation("Barguna", 22.1555, 90.1235, division = "Barisal"),
        PresetLocation("Bhola", 22.6859, 90.6440, division = "Barisal"),
        PresetLocation("Jhalokati", 22.6438, 90.1981, division = "Barisal"),
        PresetLocation("Patuakhali", 22.3533, 90.3167, division = "Barisal"),
        PresetLocation("Pirojpur", 22.5791, 89.9751, division = "Barisal"),

        PresetLocation("Rangpur", 25.7558, 89.2447, division = "Rangpur"),
        PresetLocation("Dinajpur", 25.6217, 88.6354, division = "Rangpur"),
        PresetLocation("Gaibandha", 25.3283, 89.5428, division = "Rangpur"),
        PresetLocation("Kurigram", 25.8054, 89.6361, division = "Rangpur"),
        PresetLocation("Lalmonirhat", 25.9123, 89.4442, division = "Rangpur"),
        PresetLocation("Nilphamari", 25.9317, 88.8560, division = "Rangpur"),
        PresetLocation("Panchagarh", 26.3411, 88.5539, division = "Rangpur"),
        PresetLocation("Thakurgaon", 26.0337, 88.4617, division = "Rangpur"),

        PresetLocation("Mymensingh", 24.7471, 90.4203, division = "Mymensingh"),
        PresetLocation("Jamalpur", 24.9375, 89.9377, division = "Mymensingh"),
        PresetLocation("Netrokona", 24.8781, 90.7275, division = "Mymensingh"),
        PresetLocation("Sherpur", 25.0189, 90.0175, division = "Mymensingh"),

        // Major International Locations
        PresetLocation("London", 51.5074, -0.1278, isInternational = true),
        PresetLocation("New York", 40.7128, -74.0060, isInternational = true),
        PresetLocation("Tokyo", 35.6762, 139.6503, isInternational = true),
        PresetLocation("Dubai", 25.2048, 55.2708, isInternational = true),
        PresetLocation("New Delhi", 28.6139, 77.2090, isInternational = true),
        PresetLocation("Sydney", -33.8688, 151.2093, isInternational = true),
        PresetLocation("Singapore", 1.3521, 103.8198, isInternational = true),
        PresetLocation("Kuala Lumpur", 3.1390, 101.6869, isInternational = true),
        PresetLocation("Riyadh", 24.7136, 46.6753, isInternational = true),
        PresetLocation("Kolkata", 22.5726, 88.3639, isInternational = true),
        PresetLocation("Karachi", 24.8607, 67.0011, isInternational = true)
    )
}
