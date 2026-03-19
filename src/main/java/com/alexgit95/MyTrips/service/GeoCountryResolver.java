package com.alexgit95.MyTrips.service;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Resolves country ISO code + subdivision (French dept / US state) from GPS coordinates.
 *
 * Two modes, controlled by the property {@code app.geo.api-enabled}:
 *
 *  • API mode  (app.geo.api-enabled=true)  – calls BigDataCloud free reverse-geocoding API.
 *    Enable in Portainer by setting the env variable: GEO_API_ENABLED=true
 *
 *  • Local mode (app.geo.api-enabled=false) – pure in-memory resolution using bounding boxes
 *    with centroid-distance tiebreaking at borders. No network call.
 *
 * Results are always cached in memory to avoid redundant operations.
 */
@Service
public class GeoCountryResolver {

    private static final Logger log = LoggerFactory.getLogger(GeoCountryResolver.class);

    @Value("${app.geo.api-enabled:false}")
    private boolean apiEnabled;

    private RestClient restClient;

    @PostConstruct
    public void init() {
        if (apiEnabled) {
            this.restClient = RestClient.builder()
                    .baseUrl("https://api.bigdatacloud.net")
                    .defaultHeader("Accept", "application/json")
                    .build();
            log.info("GeoCountryResolver: mode API (BigDataCloud)");
        } else {
            log.info("GeoCountryResolver: mode LOCAL (bounding boxes + centroid)");
        }
    }

    /** @return human-readable mode label for the admin page */
    public String getMode() {
        return apiEnabled
                ? "API BigDataCloud (résolution précise en ligne)"
                : "Local (bounding boxes + centroïdes, hors-ligne)";
    }

    public boolean isApiEnabled() {
        return apiEnabled;
    }

    /**
     * Runtime override – called from the admin panel to switch mode without restart.
     * Clears the result cache so the next call re-resolves with the new mode.
     */
    public synchronized void setApiEnabled(boolean enabled) {
        if (this.apiEnabled == enabled) return;
        this.apiEnabled = enabled;
        // Lazy-init RestClient the first time API mode is activated
        if (enabled && this.restClient == null) {
            this.restClient = RestClient.builder()
                    .baseUrl("https://api.bigdatacloud.net")
                    .defaultHeader("Accept", "application/json")
                    .build();
        }
        cache.clear();
        log.info("GeoCountryResolver: switched to {} mode (admin override)",
                enabled ? "API" : "LOCAL");
    }

    // =========================================================================
    // Public resolution API
    // =========================================================================

    public record GeoResult(String isoCode, String principalSubdivision,
                             String principalSubdivisionCode, String postcode) {}

    private final Map<String, Optional<GeoResult>> cache = new ConcurrentHashMap<>();

    public String resolve(Double lat, Double lng) {
        return fetch(lat, lng).map(GeoResult::isoCode).orElse(null);
    }

    public String resolveSubdivision(String isoCode, double lat, double lng) {
        Optional<GeoResult> opt = fetch(lat, lng);
        if (opt.isEmpty()) return null;
        GeoResult r = opt.get();
        if ("FR".equals(isoCode)) return resolveFranceDept(r, lat, lng);
        if ("US".equals(isoCode)) return resolveUsaState(r);
        return null;
    }

    // =========================================================================
    // Internal dispatch (cache + mode)
    // =========================================================================

    private Optional<GeoResult> fetch(Double lat, Double lng) {
        if (lat == null || lng == null) return Optional.empty();
        String key = String.format("%.3f,%.3f", lat, lng);
        return cache.computeIfAbsent(key, k ->
                apiEnabled ? fetchFromApi(lat, lng) : fetchLocal(lat, lng));
    }

    // =========================================================================
    // API mode – BigDataCloud
    // =========================================================================

    private record BigDataCloudResponse(
            String countryCode, String countryName,
            String principalSubdivision, String principalSubdivisionCode,
            String postcode, String city) {}

    private Optional<GeoResult> fetchFromApi(double lat, double lng) {
        try {
            BigDataCloudResponse resp = restClient.get()
                    .uri("/data/reverse-geocode-client?latitude={lat}&longitude={lng}&localityLanguage=fr",
                            lat, lng)
                    .retrieve()
                    .body(BigDataCloudResponse.class);
            if (resp == null || resp.countryCode() == null || resp.countryCode().isBlank())
                return Optional.empty();
            return Optional.of(new GeoResult(
                    resp.countryCode(), resp.principalSubdivision(),
                    resp.principalSubdivisionCode(), resp.postcode()));
        } catch (Exception e) {
            log.warn("BigDataCloud API error ({},{}): {}", lat, lng, e.getMessage());
            // Graceful fallback to local on API failure
            return fetchLocal(lat, lng);
        }
    }

    // =========================================================================
    // Local mode – bounding boxes + centroid-distance tiebreaker
    // =========================================================================

    private record BBox(double minLat, double maxLat, double minLng, double maxLng) {
        boolean contains(double lat, double lng) {
            return lat >= minLat && lat <= maxLat && lng >= minLng && lng <= maxLng;
        }
    }

    /** Country entry: ISO code, centroid lat, centroid lng, list of bboxes */
    private record CountryEntry(String iso, double cLat, double cLng, List<BBox> bboxes) {}

    private static final List<CountryEntry> COUNTRIES = new ArrayList<>();

    static {
        // ---- Micro-states (always checked first — bboxes tiny) ----
        c("MC", 43.738, 7.422, b(43.724,43.752,7.405,7.440));
        c("SM", 43.942, 12.458, b(43.893,43.993,12.400,12.517));
        c("VA", 41.904, 12.452, b(41.900,41.908,12.444,12.458));
        c("LI", 47.166, 9.555, b(47.047,47.270,9.470,9.636));
        c("AD", 42.546, 1.601, b(42.428,42.656,1.408,1.787));
        c("MT", 35.937, 14.375, b(35.786,36.082,14.183,14.576));
        c("LU", 49.815, 6.130, b(49.441,50.184,5.734,6.532));
        c("SG", 1.352, 103.820, b(1.166,1.471,103.605,104.085));
        c("BH", 26.066, 50.558, b(25.794,26.330,50.455,50.645));
        c("QA", 25.354, 51.184, b(24.556,26.155,50.752,51.607));
        // ---- Europe ----
        c("AL", 41.153, 20.168, b(39.644,42.661,19.263,21.055));
        c("AM", 40.069, 45.038, b(38.840,41.300,43.447,46.637));
        c("AT", 47.516, 14.550, b(46.372,49.021,9.530,17.161));
        c("AZ", 40.143, 47.577, b(38.392,41.906,44.764,50.393));
        c("BA", 43.917, 17.679, b(42.556,45.268,15.729,19.625));
        c("BE", 50.503, 4.469, b(49.497,51.505,2.544,6.408));
        c("BG", 42.734, 25.486, b(41.235,44.215,22.371,28.609));
        c("BY", 53.710, 27.953, b(51.262,56.172,23.178,32.776));
        c("CH", 46.818, 8.228, b(45.817,47.808,5.956,10.492));
        c("CY", 35.126, 33.430, b(34.631,35.707,32.268,34.597));
        c("CZ", 49.818, 15.473, b(48.551,51.056,12.090,18.860));
        c("DE", 51.166, 10.452, b(47.270,55.059,5.866,15.042));
        c("DK", 56.263, 9.502, b(54.559,57.748,8.072,15.197));
        c("EE", 58.596, 25.014, b(57.508,59.685,20.972,28.211));
        c("ES", 40.464, -3.749,
                b(35.946,43.793,-9.302,4.328),   // Péninsule + Baléares
                b(27.637,29.416,-18.162,-13.421)); // Canaries
        c("FI", 61.924, 25.748, b(59.808,70.093,19.317,31.588));
        c("FR", 46.227, 2.214,
                b(41.333,51.124,-5.142,9.561),    // Métropole + Corse
                b(14.393,16.515,-61.809,-60.809), // Guadeloupe
                b(14.393,14.878,-61.228,-60.810), // Martinique
                b(2.113,5.751,-54.600,-51.630));  // Guyane
        c("GB", 55.378, -3.436, b(49.674,61.061,-8.650,1.767));
        c("GE", 42.315, 43.357, b(41.054,43.586,39.976,46.739));
        c("GR", 39.074, 21.824,
                b(34.803,41.751,19.373,28.248));
        c("HR", 45.100, 15.200, b(42.392,46.555,13.489,19.449));
        c("HU", 47.163, 19.503, b(45.742,48.585,16.113,22.899));
        c("IE", 53.413, -8.244, b(51.422,55.387,-10.481,-5.994));
        c("IS", 64.963, -19.021, b(63.297,66.563,-24.540,-13.495));
        c("IT", 41.872, 12.568,
                b(35.491,47.095,6.627,18.521));
        c("LT", 55.170, 23.881, b(53.900,56.450,20.934,26.836));
        c("LV", 56.879, 24.604, b(55.675,57.975,20.972,28.241));
        c("MD", 47.412, 28.370, b(45.467,48.492,26.616,30.135));
        c("ME", 42.709, 19.374, b(41.849,43.559,18.435,20.359));
        c("MK", 41.608, 21.745, b(41.068,42.371,20.453,22.956));
        c("NL", 52.133, 5.291, b(50.750,53.555,3.358,7.228));
        c("NO", 60.472, 8.469, b(57.985,71.184,4.637,31.044));
        c("PL", 51.920, 19.145, b(49.002,54.836,14.122,24.146));
        c("PT", 39.400, -8.225,
                b(36.838,42.154,-9.526,-6.190),
                b(36.924,39.310,-31.268,-25.011),  // Açores
                b(32.633,33.121,-17.267,-16.273)); // Madère
        c("RO", 45.943, 24.967, b(43.619,48.265,20.261,29.757));
        c("RS", 44.017, 21.006, b(42.232,46.190,18.826,23.005));
        c("RU", 61.524, 105.319, b(41.188,81.858,19.643,180.0));
        c("SE", 60.128, 18.644, b(55.337,69.061,11.118,24.161));
        c("SI", 46.152, 14.995, b(45.421,46.877,13.375,16.611));
        c("SK", 48.669, 19.699, b(47.728,49.614,16.833,22.559));
        c("TR", 38.964, 35.243, b(35.817,42.107,25.669,44.818));
        c("UA", 48.380, 31.165, b(44.386,52.380,22.137,40.228));
        c("XK", 42.602, 20.903, b(41.857,43.270,20.014,21.789));
        // ---- Moyen-Orient ----
        c("AE", 23.424, 53.848, b(22.633,26.085,51.579,56.381));
        c("AF", 33.939, 67.710, b(29.378,38.491,60.506,74.880));
        c("IL", 31.047, 34.852, b(29.497,33.337,34.267,35.895));
        c("IQ", 33.224, 43.679, b(29.061,37.385,38.795,48.576));
        c("IR", 32.428, 53.688, b(25.065,39.778,44.032,63.316));
        c("JO", 30.586, 36.238, b(29.183,33.374,34.959,39.301));
        c("KW", 29.311, 47.481, b(28.526,30.089,46.568,48.416));
        c("LB", 33.855, 35.862, b(33.055,34.693,35.101,36.624));
        c("OM", 21.513, 55.923, b(16.646,26.396,51.994,59.833));
        c("PS", 31.952, 35.233, b(31.216,32.554,34.216,35.573));
        c("SA", 23.886, 45.079, b(16.346,32.161,34.632,55.667));
        c("SY", 34.802, 38.997, b(32.312,37.328,35.727,42.376));
        c("YE", 15.553, 48.516, b(12.109,19.000,42.532,53.109));
        // ---- Afrique ----
        c("DZ", 28.033, 1.659, b(18.968,37.094,-8.673,11.979));
        c("AO", -11.203, 17.874, b(-18.020,-4.387,11.640,24.082));
        c("BF", 12.365, -1.562, b(9.401,15.085,-5.521,2.403));
        c("BI", -3.382, 29.919, b(-4.469,-2.309,28.988,30.848));
        c("BW", -22.328, 24.684, b(-26.909,-17.780,19.999,29.375));
        c("CD", -4.038, 21.759, b(-13.460,5.385,12.180,31.305));
        c("CG", -0.228, 15.827, b(-5.013,3.703,11.208,18.648));
        c("CI", 7.540, -5.547, b(4.361,10.740,-8.601,-2.493));
        c("CM", 7.370, 12.355, b(1.653,13.083,8.499,16.013));
        c("EG", 26.820, 30.802, b(21.995,31.669,24.700,36.895));
        c("ET", 9.145, 40.490, b(3.406,14.854,32.997,47.979));
        c("GA", -0.804, 11.610, b(-3.978,2.318,8.698,14.437));
        c("GH", 7.946, -1.023, b(4.738,11.175,-3.262,1.187));
        c("GN", 9.946, -11.347, b(7.194,12.675,-15.131,-7.637));
        c("KE", -0.023, 37.906, b(-4.677,4.620,33.909,41.899));
        c("LY", 26.335, 17.228, b(19.502,33.168,9.391,25.151));
        c("MA", 31.791, -7.092, b(27.667,35.923,-13.168,-0.991));
        c("MG", -18.767, 46.869, b(-25.601,-11.945,43.225,50.480));
        c("ML", 17.570, -3.996, b(10.160,25.000,-4.245,4.245));
        c("MR", 21.007, -10.940, b(14.721,27.295,-17.068,-4.833));
        c("MU", -20.348, 57.552, b(-20.520,-19.992,57.290,57.800));
        c("MZ", -18.666, 35.505, b(-26.868,-10.471,30.213,40.842));
        c("NA", -22.958, 18.490, b(-28.970,-16.959,11.716,25.260));
        c("NE", 17.608, 8.082, b(11.696,23.525,0.166,15.996));
        c("NG", 9.082, 8.676, b(4.277,13.893,2.668,14.678));
        c("RE", -21.115, 55.536, b(-21.390,-20.870,55.215,55.838));
        c("RW", -1.940, 29.874, b(-2.840,-1.052,28.861,30.899));
        c("SD", 12.863, 30.218, b(8.685,22.232,21.826,38.607));
        c("SN", 14.497, -14.452, b(12.308,16.692,-17.535,-11.355));
        c("SO", 5.152, 46.199, b(-1.664,11.978,40.986,51.413));
        c("TD", 15.454, 18.733, b(7.441,23.525,13.473,24.000));
        c("TN", 33.886, 9.538, b(30.240,37.540,7.524,11.599));
        c("TZ", -6.369, 34.889, b(-11.745,-0.990,29.327,40.443));
        c("UG", 1.373, 32.290, b(-1.476,4.234,29.574,35.000));
        c("ZA", -30.559, 22.937, b(-34.820,-22.126,16.458,32.892));
        c("ZM", -13.133, 27.849, b(-18.080,-8.224,21.999,33.706));
        c("ZW", -19.015, 29.154, b(-22.418,-15.609,25.237,33.055));
        // ---- Asie ----
        c("BD", 23.685, 90.356, b(20.741,26.634,88.029,92.673));
        c("BT", 27.515, 90.434, b(26.720,28.324,88.747,92.123));
        c("CN", 35.861, 104.196, b(18.153,53.557,73.499,134.772));
        c("HK", 22.321, 114.170, b(22.152,22.561,113.837,114.406));
        c("ID", -0.790, 113.922, b(-10.941,5.906,95.298,141.022));
        c("IN", 20.594, 78.963, b(6.754,35.674,68.176,97.403));
        c("JP", 36.205, 138.253, b(24.396,45.552,122.934,153.987));
        c("KG", 41.204, 74.766, b(39.193,43.238,69.464,80.283));
        c("KH", 12.566, 104.991, b(10.487,14.690,102.338,107.628));
        c("KP", 40.340, 127.511, b(37.674,42.985,124.181,130.680));
        c("KR", 35.908, 127.767, b(33.106,38.614,125.887,129.584));
        c("KZ", 48.020, 66.924, b(40.566,55.448,50.270,87.360));
        c("LA", 17.973, 102.495, b(13.910,22.502,100.093,107.636));
        c("LK", 7.873, 80.772, b(5.917,9.835,79.652,81.879));
        c("MM", 19.165, 96.871, b(9.784,28.548,92.189,101.170));
        c("MN", 46.863, 103.847, b(41.567,52.149,87.760,119.935));
        c("MY", 4.211, 108.010, b(0.855,7.363,99.643,119.267));
        c("NP", 28.395, 84.124, b(26.347,30.448,80.058,88.201));
        c("PH", 12.880, 121.774, b(4.587,21.120,116.928,126.537));
        c("PK", 30.375, 69.346, b(23.694,37.097,60.876,77.841));
        c("TH", 15.870, 100.993, b(5.613,20.465,97.343,105.640));
        c("TJ", 38.861, 71.276, b(36.674,41.044,67.344,75.158));
        c("TM", 39.120, 59.556, b(35.140,42.798,52.440,66.685));
        c("TW", 23.697, 120.961, b(21.897,25.299,119.538,122.006));
        c("UZ", 41.377, 64.586, b(37.183,45.590,55.998,73.148));
        c("VN", 14.059, 108.277, b(8.563,23.393,102.145,109.465));
        // ---- Amérique du Nord ----
        c("BZ", 17.189, -88.498, b(15.886,18.496,-89.225,-87.770));
        c("CA", 56.131, -106.347, b(41.676,83.111,-141.002,-52.620));
        c("CR", 9.748, -83.754, b(7.986,11.218,-85.942,-82.552));
        c("CU", 21.522, -77.782, b(19.826,23.188,-84.950,-74.131));
        c("DO", 18.736, -70.163, b(17.470,19.931,-72.002,-68.323));
        c("GT", 15.784, -90.231, b(13.738,17.816,-92.232,-88.225));
        c("HN", 14.791, -86.242, b(12.982,16.518,-89.348,-83.148));
        c("HT", 18.971, -72.285, b(18.023,20.089,-74.479,-71.623));
        c("JM", 18.109, -77.298, b(17.703,18.525,-77.985,-76.183));
        c("MX", 23.635, -102.553, b(14.532,32.719,-117.124,-86.744));
        c("NI", 12.866, -85.208, b(10.708,15.024,-87.668,-82.579));
        c("PA", 8.538, -80.783, b(7.204,9.648,-83.051,-77.158));
        c("US", 37.090, -95.713,
                b(24.396,49.384,-124.733,-66.934),  // Contig.
                b(51.000,71.391,-168.0,-130.0),      // Alaska
                b(18.910,28.402,-178.340,-154.806)); // Hawaï
        // ---- Amérique du Sud ----
        c("AR", -38.416, -63.617, b(-55.058,-21.781,-73.560,-53.637));
        c("BO", -16.290, -63.589, b(-22.895,-9.669,-69.645,-57.453));
        c("BR", -14.235, -51.925, b(-33.751,5.272,-73.982,-28.848));
        c("CL", -35.675, -71.543, b(-55.900,-17.508,-75.644,-66.418));
        c("CO", 4.571, -74.297, b(-4.226,12.437,-78.999,-66.874));
        c("EC", -1.832, -78.183, b(-4.996,1.680,-80.927,-75.193));
        c("GY", 4.860, -58.930, b(1.185,8.554,-61.411,-56.480));
        c("PE", -9.190, -75.016, b(-18.348,-0.038,-81.328,-68.677));
        c("PY", -23.443, -58.444, b(-27.591,-19.287,-62.644,-54.258));
        c("SR", 3.919, -56.027, b(1.837,6.003,-58.083,-53.977));
        c("UY", -32.523, -55.765, b(-34.952,-30.190,-58.440,-53.093));
        c("VE", 6.424, -66.590, b(0.723,12.197,-73.354,-59.758));
        // ---- Océanie ----
        c("AU", -25.274, 133.775, b(-43.658,-9.221,113.338,153.639));
        c("FJ", -17.713, 178.065, b(-20.676,-15.717,177.141,180.0));
        c("NC", -20.904, 165.618, b(-22.696,-19.552,164.019,167.120));
        c("NZ", -40.901, 174.886, b(-52.620,-29.230,166.426,178.574));
        c("PF", -17.680, -149.406, b(-27.666,-8.534,-154.766,-134.940));
        c("PG", -6.315, 143.956, b(-11.658,-0.879,140.842,155.964));
    }

    @SafeVarargs
    private static void c(String iso, double cLat, double cLng, BBox... bboxes) {
        COUNTRIES.add(new CountryEntry(iso, cLat, cLng, List.of(bboxes)));
    }

    private static BBox b(double minLat, double maxLat, double minLng, double maxLng) {
        return new BBox(minLat, maxLat, minLng, maxLng);
    }

    private Optional<GeoResult> fetchLocal(double lat, double lng) {
        List<CountryEntry> candidates = new ArrayList<>();
        for (CountryEntry entry : COUNTRIES) {
            for (BBox bbox : entry.bboxes()) {
                if (bbox.contains(lat, lng)) {
                    candidates.add(entry);
                    break;
                }
            }
        }
        if (candidates.isEmpty()) return Optional.empty();
        if (candidates.size() == 1) {
            return Optional.of(new GeoResult(candidates.get(0).iso(), null, null, null));
        }
        // Tiebreaker: country whose centroid is closest (Haversine)
        CountryEntry best = candidates.stream()
                .min(Comparator.comparingDouble(e -> haversine(lat, lng, e.cLat(), e.cLng())))
                .orElseThrow();
        return Optional.of(new GeoResult(best.iso(), null, null, null));
    }

    /** Haversine distance in km */
    private static double haversine(double lat1, double lng1, double lat2, double lng2) {
        double R = 6371;
        double dLat = Math.toRadians(lat2 - lat1);
        double dLng = Math.toRadians(lng2 - lng1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLng / 2) * Math.sin(dLng / 2);
        return R * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    }

    // =========================================================================
    // Subdivision resolvers
    // =========================================================================

    private String resolveFranceDept(GeoResult r, double lat, double lng) {
        // API mode: use postcode
        if (r.postcode() != null && !r.postcode().isBlank()) {
            return deptFromPostcode(r.postcode());
        }
        // Local mode or no postcode: use dept bbox table
        return resolveFranceDeptLocal(lat, lng);
    }

    private String deptFromPostcode(String postcode) {
        if (postcode.length() >= 3) {
            String d3 = postcode.substring(0, 3);
            switch (d3) {
                case "971": return "Guadeloupe";
                case "972": return "Martinique";
                case "973": return "Guyane";
                case "974": return "La Réunion";
                case "976": return "Mayotte";
            }
        }
        if (postcode.startsWith("20") && postcode.length() >= 5) {
            try {
                return Integer.parseInt(postcode.substring(0, 5)) <= 20190
                        ? "Corse-du-Sud" : "Haute-Corse";
            } catch (NumberFormatException ignored) {}
        }
        if (postcode.length() >= 2) {
            String name = DEPT_NUM_TO_NAME.get(postcode.substring(0, 2));
            if (name != null) return name;
        }
        return null;
    }

    // France dept bboxes for local mode
    private record DeptBox(String name, BBox bbox) {}
    private static final List<DeptBox> FRANCE_DEPTS = new ArrayList<>();
    static {
        dept("Ain",                  45.25,46.52,4.77,6.03);
        dept("Aisne",                49.15,50.07,2.97,4.27);
        dept("Allier",               46.00,46.75,2.60,4.00);
        dept("Alpes-de-Haute-Provence",43.65,44.70,5.65,6.89);
        dept("Hautes-Alpes",         44.22,45.14,5.61,6.98);
        dept("Alpes-Maritimes",      43.47,44.36,6.63,7.72);
        dept("Ardèche",              44.29,45.37,3.97,4.89);
        dept("Ardennes",             49.37,50.09,4.03,5.36);
        dept("Ariège",               42.67,43.34,0.92,2.24);
        dept("Aube",                 47.99,48.74,3.52,4.85);
        dept("Aude",                 42.65,43.46,1.72,3.23);
        dept("Aveyron",              43.56,44.86,1.84,3.33);
        dept("Bouches-du-Rhône",     43.17,43.96,4.23,5.59);
        dept("Calvados",             48.73,49.38,-1.07,0.55);
        dept("Cantal",               44.60,45.56,2.08,3.32);
        dept("Charente",             45.23,46.11,-0.40,0.94);
        dept("Charente-Maritime",    45.09,46.37,-1.60,-0.02);
        dept("Cher",                 46.36,47.62,1.76,3.05);
        dept("Corrèze",              44.88,45.86,1.38,2.68);
        dept("Corse-du-Sud",         41.33,42.38,8.54,9.56);
        dept("Haute-Corse",          41.96,43.03,8.63,9.56);
        dept("Côte-d'Or",            46.88,48.05,4.05,5.53);
        dept("Côtes-d'Armor",        47.90,48.78,-2.99,-1.54);
        dept("Creuse",               45.72,46.41,1.57,2.71);
        dept("Dordogne",             44.50,45.72,0.23,1.75);
        dept("Doubs",                46.79,47.75,5.83,7.08);
        dept("Drôme",                44.11,45.32,4.63,5.80);
        dept("Eure",                 48.76,49.46,0.48,2.00);
        dept("Eure-et-Loir",         47.82,48.93,0.79,2.00);
        dept("Finistère",            47.71,48.77,-5.14,-3.53);
        dept("Gard",                 43.46,44.44,3.26,4.89);
        dept("Haute-Garonne",        42.67,43.93,0.52,2.31);
        dept("Gers",                 43.47,44.07,-0.14,1.12);
        dept("Gironde",              44.19,45.57,-1.26,0.38);
        dept("Hérault",              43.22,43.94,2.81,4.11);
        dept("Ille-et-Vilaine",      47.62,48.72,-2.28,-0.96);
        dept("Indre",                46.35,47.18,0.96,2.18);
        dept("Indre-et-Loire",       46.88,47.71,0.07,1.28);
        dept("Isère",                44.73,45.93,4.72,6.32);
        dept("Jura",                 46.29,47.59,5.31,6.21);
        dept("Landes",               43.46,44.54,-1.49,0.11);
        dept("Loir-et-Cher",         47.24,48.15,0.72,2.30);
        dept("Loire",                45.27,46.28,3.74,4.76);
        dept("Haute-Loire",          44.71,45.57,3.10,4.32);
        dept("Loire-Atlantique",     46.83,47.82,-2.55,-0.90);
        dept("Loiret",               47.49,48.45,1.60,3.12);
        dept("Lot",                  44.25,45.06,1.14,2.36);
        dept("Lot-et-Garonne",       43.89,44.73,0.13,1.09);
        dept("Lozère",               44.10,44.99,2.97,3.92);
        dept("Maine-et-Loire",       46.93,47.82,-1.60,0.02);
        dept("Manche",               48.45,49.72,-1.90,-0.85);
        dept("Marne",                48.51,49.40,3.40,5.05);
        dept("Haute-Marne",          47.53,48.56,4.65,5.98);
        dept("Mayenne",              47.84,48.61,-1.18,0.09);
        dept("Meurthe-et-Moselle",   48.60,49.61,5.70,7.08);
        dept("Meuse",                48.44,49.75,4.97,5.97);
        dept("Morbihan",             47.34,48.11,-3.68,-1.93);
        dept("Moselle",              48.76,49.99,6.08,7.64);
        dept("Nièvre",               46.62,47.62,2.89,4.00);
        dept("Nord",                 50.00,51.09,2.54,4.23);
        dept("Oise",                 49.14,49.97,1.62,3.32);
        dept("Orne",                 48.10,48.92,-0.82,0.84);
        dept("Pas-de-Calais",        50.02,51.00,1.56,3.01);
        dept("Puy-de-Dôme",          45.27,46.16,2.56,3.88);
        dept("Pyrénées-Atlantiques", 42.78,43.79,-1.77,0.49);
        dept("Hautes-Pyrénées",      42.67,43.55,-0.26,0.82);
        dept("Pyrénées-Orientales",  42.33,43.09,1.72,3.17);
        dept("Bas-Rhin",             48.06,49.07,7.37,8.24);
        dept("Haut-Rhin",            47.43,48.42,6.82,7.63);
        dept("Rhône",                45.46,46.31,4.23,5.16);
        dept("Haute-Saône",          47.29,48.07,5.38,6.84);
        dept("Saône-et-Loire",       46.16,47.14,3.80,5.49);
        dept("Sarthe",               47.64,48.56,-0.56,0.98);
        dept("Savoie",               45.11,46.10,5.83,7.18);
        dept("Haute-Savoie",         45.68,46.68,5.79,7.04);
        dept("Paris",                48.81,48.90,2.22,2.47);
        dept("Seine-Maritime",       49.27,50.07,-0.37,1.80);
        dept("Seine-et-Marne",       48.12,49.11,2.40,3.56);
        dept("Yvelines",             48.46,49.09,1.44,2.19);
        dept("Deux-Sèvres",          45.93,47.09,-0.77,0.30);
        dept("Somme",                49.59,50.37,1.46,3.23);
        dept("Tarn",                 43.47,44.13,1.59,2.80);
        dept("Tarn-et-Garonne",      43.78,44.37,0.91,1.98);
        dept("Var",                  43.00,43.80,5.65,6.93);
        dept("Vaucluse",             43.67,44.39,4.63,5.74);
        dept("Vendée",               46.27,47.13,-2.40,-0.67);
        dept("Vienne",               46.18,47.17,0.02,1.17);
        dept("Haute-Vienne",         45.42,46.18,0.87,2.18);
        dept("Vosges",               47.72,48.83,5.58,7.22);
        dept("Yonne",                47.35,48.40,2.89,4.34);
        dept("Territoire de Belfort",47.41,47.79,6.82,7.19);
        dept("Essonne",              48.29,48.74,1.91,2.60);
        dept("Hauts-de-Seine",       48.81,48.93,2.16,2.34);
        dept("Seine-Saint-Denis",    48.85,49.01,2.33,2.59);
        dept("Val-de-Marne",         48.70,48.88,2.32,2.58);
        dept("Val-d'Oise",           48.93,49.25,1.68,2.57);
        dept("Guadeloupe",           15.83,16.52,-61.81,-60.81);
        dept("Martinique",           14.39,14.88,-61.23,-60.81);
        dept("Guyane",               2.11,5.75,-54.60,-51.63);
        dept("La Réunion",           -21.39,-20.87,55.21,55.84);
        dept("Mayotte",              -13.00,-12.64,45.03,45.30);
    }

    private static void dept(String name, double minLat, double maxLat,
                              double minLng, double maxLng) {
        FRANCE_DEPTS.add(new DeptBox(name, new BBox(minLat, maxLat, minLng, maxLng)));
    }

    private String resolveFranceDeptLocal(double lat, double lng) {
        // All matching depts → pick nearest centroid (mid-point of bbox)
        return FRANCE_DEPTS.stream()
                .filter(d -> d.bbox().contains(lat, lng))
                .min(Comparator.comparingDouble(d -> {
                    BBox bx = d.bbox();
                    double cLat = (bx.minLat() + bx.maxLat()) / 2;
                    double cLng = (bx.minLng() + bx.maxLng()) / 2;
                    return haversine(lat, lng, cLat, cLng);
                }))
                .map(DeptBox::name)
                .orElse(null);
    }

    private String resolveUsaState(GeoResult r) {
        String code = r.principalSubdivisionCode();
        if (code != null && code.startsWith("US-") && code.length() >= 5) {
            String name = USA_STATE_CANONICAL.get(code.substring(3));
            if (name != null) return name;
        }
        return r.principalSubdivision();
    }

    // =========================================================================
    // Static lookup tables
    // =========================================================================

    private static final Map<String, String> DEPT_NUM_TO_NAME = new HashMap<>();
    static {
        DEPT_NUM_TO_NAME.put("01","Ain"); DEPT_NUM_TO_NAME.put("02","Aisne");
        DEPT_NUM_TO_NAME.put("03","Allier"); DEPT_NUM_TO_NAME.put("04","Alpes-de-Haute-Provence");
        DEPT_NUM_TO_NAME.put("05","Hautes-Alpes"); DEPT_NUM_TO_NAME.put("06","Alpes-Maritimes");
        DEPT_NUM_TO_NAME.put("07","Ardèche"); DEPT_NUM_TO_NAME.put("08","Ardennes");
        DEPT_NUM_TO_NAME.put("09","Ariège"); DEPT_NUM_TO_NAME.put("10","Aube");
        DEPT_NUM_TO_NAME.put("11","Aude"); DEPT_NUM_TO_NAME.put("12","Aveyron");
        DEPT_NUM_TO_NAME.put("13","Bouches-du-Rhône"); DEPT_NUM_TO_NAME.put("14","Calvados");
        DEPT_NUM_TO_NAME.put("15","Cantal"); DEPT_NUM_TO_NAME.put("16","Charente");
        DEPT_NUM_TO_NAME.put("17","Charente-Maritime"); DEPT_NUM_TO_NAME.put("18","Cher");
        DEPT_NUM_TO_NAME.put("19","Corrèze"); DEPT_NUM_TO_NAME.put("21","Côte-d'Or");
        DEPT_NUM_TO_NAME.put("22","Côtes-d'Armor"); DEPT_NUM_TO_NAME.put("23","Creuse");
        DEPT_NUM_TO_NAME.put("24","Dordogne"); DEPT_NUM_TO_NAME.put("25","Doubs");
        DEPT_NUM_TO_NAME.put("26","Drôme"); DEPT_NUM_TO_NAME.put("27","Eure");
        DEPT_NUM_TO_NAME.put("28","Eure-et-Loir"); DEPT_NUM_TO_NAME.put("29","Finistère");
        DEPT_NUM_TO_NAME.put("30","Gard"); DEPT_NUM_TO_NAME.put("31","Haute-Garonne");
        DEPT_NUM_TO_NAME.put("32","Gers"); DEPT_NUM_TO_NAME.put("33","Gironde");
        DEPT_NUM_TO_NAME.put("34","Hérault"); DEPT_NUM_TO_NAME.put("35","Ille-et-Vilaine");
        DEPT_NUM_TO_NAME.put("36","Indre"); DEPT_NUM_TO_NAME.put("37","Indre-et-Loire");
        DEPT_NUM_TO_NAME.put("38","Isère"); DEPT_NUM_TO_NAME.put("39","Jura");
        DEPT_NUM_TO_NAME.put("40","Landes"); DEPT_NUM_TO_NAME.put("41","Loir-et-Cher");
        DEPT_NUM_TO_NAME.put("42","Loire"); DEPT_NUM_TO_NAME.put("43","Haute-Loire");
        DEPT_NUM_TO_NAME.put("44","Loire-Atlantique"); DEPT_NUM_TO_NAME.put("45","Loiret");
        DEPT_NUM_TO_NAME.put("46","Lot"); DEPT_NUM_TO_NAME.put("47","Lot-et-Garonne");
        DEPT_NUM_TO_NAME.put("48","Lozère"); DEPT_NUM_TO_NAME.put("49","Maine-et-Loire");
        DEPT_NUM_TO_NAME.put("50","Manche"); DEPT_NUM_TO_NAME.put("51","Marne");
        DEPT_NUM_TO_NAME.put("52","Haute-Marne"); DEPT_NUM_TO_NAME.put("53","Mayenne");
        DEPT_NUM_TO_NAME.put("54","Meurthe-et-Moselle"); DEPT_NUM_TO_NAME.put("55","Meuse");
        DEPT_NUM_TO_NAME.put("56","Morbihan"); DEPT_NUM_TO_NAME.put("57","Moselle");
        DEPT_NUM_TO_NAME.put("58","Nièvre"); DEPT_NUM_TO_NAME.put("59","Nord");
        DEPT_NUM_TO_NAME.put("60","Oise"); DEPT_NUM_TO_NAME.put("61","Orne");
        DEPT_NUM_TO_NAME.put("62","Pas-de-Calais"); DEPT_NUM_TO_NAME.put("63","Puy-de-Dôme");
        DEPT_NUM_TO_NAME.put("64","Pyrénées-Atlantiques"); DEPT_NUM_TO_NAME.put("65","Hautes-Pyrénées");
        DEPT_NUM_TO_NAME.put("66","Pyrénées-Orientales"); DEPT_NUM_TO_NAME.put("67","Bas-Rhin");
        DEPT_NUM_TO_NAME.put("68","Haut-Rhin"); DEPT_NUM_TO_NAME.put("69","Rhône");
        DEPT_NUM_TO_NAME.put("70","Haute-Saône"); DEPT_NUM_TO_NAME.put("71","Saône-et-Loire");
        DEPT_NUM_TO_NAME.put("72","Sarthe"); DEPT_NUM_TO_NAME.put("73","Savoie");
        DEPT_NUM_TO_NAME.put("74","Haute-Savoie"); DEPT_NUM_TO_NAME.put("75","Paris");
        DEPT_NUM_TO_NAME.put("76","Seine-Maritime"); DEPT_NUM_TO_NAME.put("77","Seine-et-Marne");
        DEPT_NUM_TO_NAME.put("78","Yvelines"); DEPT_NUM_TO_NAME.put("79","Deux-Sèvres");
        DEPT_NUM_TO_NAME.put("80","Somme"); DEPT_NUM_TO_NAME.put("81","Tarn");
        DEPT_NUM_TO_NAME.put("82","Tarn-et-Garonne"); DEPT_NUM_TO_NAME.put("83","Var");
        DEPT_NUM_TO_NAME.put("84","Vaucluse"); DEPT_NUM_TO_NAME.put("85","Vendée");
        DEPT_NUM_TO_NAME.put("86","Vienne"); DEPT_NUM_TO_NAME.put("87","Haute-Vienne");
        DEPT_NUM_TO_NAME.put("88","Vosges"); DEPT_NUM_TO_NAME.put("89","Yonne");
        DEPT_NUM_TO_NAME.put("90","Territoire de Belfort"); DEPT_NUM_TO_NAME.put("91","Essonne");
        DEPT_NUM_TO_NAME.put("92","Hauts-de-Seine"); DEPT_NUM_TO_NAME.put("93","Seine-Saint-Denis");
        DEPT_NUM_TO_NAME.put("94","Val-de-Marne"); DEPT_NUM_TO_NAME.put("95","Val-d'Oise");
    }

    private static final Map<String, String> USA_STATE_CANONICAL = new HashMap<>();
    static {
        USA_STATE_CANONICAL.put("AL","Alabama"); USA_STATE_CANONICAL.put("AK","Alaska");
        USA_STATE_CANONICAL.put("AZ","Arizona"); USA_STATE_CANONICAL.put("AR","Arkansas");
        USA_STATE_CANONICAL.put("CA","Californie"); USA_STATE_CANONICAL.put("CO","Colorado");
        USA_STATE_CANONICAL.put("CT","Connecticut"); USA_STATE_CANONICAL.put("DE","Delaware");
        USA_STATE_CANONICAL.put("FL","Floride"); USA_STATE_CANONICAL.put("GA","Géorgie");
        USA_STATE_CANONICAL.put("HI","Hawaï"); USA_STATE_CANONICAL.put("ID","Idaho");
        USA_STATE_CANONICAL.put("IL","Illinois"); USA_STATE_CANONICAL.put("IN","Indiana");
        USA_STATE_CANONICAL.put("IA","Iowa"); USA_STATE_CANONICAL.put("KS","Kansas");
        USA_STATE_CANONICAL.put("KY","Kentucky"); USA_STATE_CANONICAL.put("LA","Louisiane");
        USA_STATE_CANONICAL.put("ME","Maine"); USA_STATE_CANONICAL.put("MD","Maryland");
        USA_STATE_CANONICAL.put("MA","Massachusetts"); USA_STATE_CANONICAL.put("MI","Michigan");
        USA_STATE_CANONICAL.put("MN","Minnesota"); USA_STATE_CANONICAL.put("MS","Mississippi");
        USA_STATE_CANONICAL.put("MO","Missouri"); USA_STATE_CANONICAL.put("MT","Montana");
        USA_STATE_CANONICAL.put("NE","Nebraska"); USA_STATE_CANONICAL.put("NV","Nevada");
        USA_STATE_CANONICAL.put("NH","New Hampshire"); USA_STATE_CANONICAL.put("NJ","New Jersey");
        USA_STATE_CANONICAL.put("NM","Nouveau-Mexique"); USA_STATE_CANONICAL.put("NY","New York");
        USA_STATE_CANONICAL.put("NC","Caroline du Nord"); USA_STATE_CANONICAL.put("ND","Dakota du Nord");
        USA_STATE_CANONICAL.put("OH","Ohio"); USA_STATE_CANONICAL.put("OK","Oklahoma");
        USA_STATE_CANONICAL.put("OR","Oregon"); USA_STATE_CANONICAL.put("PA","Pennsylvanie");
        USA_STATE_CANONICAL.put("RI","Rhode Island"); USA_STATE_CANONICAL.put("SC","Caroline du Sud");
        USA_STATE_CANONICAL.put("SD","Dakota du Sud"); USA_STATE_CANONICAL.put("TN","Tennessee");
        USA_STATE_CANONICAL.put("TX","Texas"); USA_STATE_CANONICAL.put("UT","Utah");
        USA_STATE_CANONICAL.put("VT","Vermont"); USA_STATE_CANONICAL.put("VA","Virginie");
        USA_STATE_CANONICAL.put("WA","Washington"); USA_STATE_CANONICAL.put("WV","Virginie-Occidentale");
        USA_STATE_CANONICAL.put("WI","Wisconsin"); USA_STATE_CANONICAL.put("WY","Wyoming");
        USA_STATE_CANONICAL.put("DC","Washington D.C.");
    }
}
