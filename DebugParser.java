import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public class DebugParser {
    public static void main(String[] args) {
        try {
            String jsonResponse = """
            {
              "success": true,
              "source": "adsbexchange",
              "quality": 0.88,
              "priority": 2,
              "responseTime": 305,
              "data": {
                "aircraft": [
                  {
                    "hex": "7C1B72",
                    "flight": "VJ2290",
                    "lat": 11.67654910655065,
                    "lon": 109.199291,
                    "alt_baro": 24807,
                    "gs": 345.2214,
                    "category": "A3"
                  }
                ],
                "total": 289,
                "ctime": 1750822365769,
                "ptime": 1750822335769
              }
            }
            """;
            
            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode root = objectMapper.readTree(jsonResponse);
            System.out.println("✅ Root JSON parsed successfully");
            
            // Handle mock API response format: {"data": {"aircraft": [...]}}
            JsonNode dataWrapper = root.path("data");
            if (!dataWrapper.isMissingNode()) {
                System.out.println("🎯 Found data wrapper, extracting...");
                root = dataWrapper; // Use data wrapper as new root
            }
            
            JsonNode aircraft = root.get("aircraft");
            System.out.println("🔍 Looking for 'aircraft' field in root: " + (aircraft != null ? "found" : "not found"));
            
            if (aircraft != null && aircraft.isArray()) {
                System.out.println("📊 Found aircraft array with " + aircraft.size() + " elements");
                
                aircraft.elements().forEachRemaining(ac -> {
                    String hex = ac.get("hex") != null ? ac.get("hex").asText() : null;
                    String flight = ac.get("flight") != null ? ac.get("flight").asText() : null;
                    Double lat = ac.get("lat") != null ? ac.get("lat").asDouble() : null;
                    Double lon = ac.get("lon") != null ? ac.get("lon").asDouble() : null;
                    
                    System.out.println("Aircraft: " + flight + " (" + hex + ") at " + lat + "," + lon);
                });
            } else {
                System.out.println("❌ No aircraft array found");
                
                // Debug: print all field names  
                java.util.List<String> fieldNames = new java.util.ArrayList<>();
                root.fieldNames().forEachRemaining(fieldNames::add);
                System.out.println("Available fields: " + fieldNames);
            }
            
        } catch (Exception e) {
            System.out.println("❌ Error: " + e.getMessage());
            e.printStackTrace();
        }
    }
} 