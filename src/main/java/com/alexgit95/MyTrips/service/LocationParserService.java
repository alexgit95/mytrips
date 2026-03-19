package com.alexgit95.MyTrips.service;

import org.springframework.stereotype.Service;

import java.util.*;

/**
 * Parses free-text location strings into structured LocationInfo objects.
 * Handles country detection, continent mapping, flag emoji, and sub-divisions
 * (French departments, US states).
 */
@Service
public class LocationParserService {

    // -------------------------------------------------------------------------
    // Inner records
    // -------------------------------------------------------------------------

    public record LocationInfo(String isoCode, String countryFr, String continent,
                               String flag, String subdivision) {}

    // -------------------------------------------------------------------------
    // Country data: iso → [frenchName, continent]
    // -------------------------------------------------------------------------

    private static final Map<String, String[]> COUNTRY_DATA = new LinkedHashMap<>();

    static {
        // Europe
        add("FR", "France", "Europe");
        add("DE", "Allemagne", "Europe");
        add("ES", "Espagne", "Europe");
        add("IT", "Italie", "Europe");
        add("PT", "Portugal", "Europe");
        add("GB", "Royaume-Uni", "Europe");
        add("BE", "Belgique", "Europe");
        add("NL", "Pays-Bas", "Europe");
        add("CH", "Suisse", "Europe");
        add("AT", "Autriche", "Europe");
        add("SE", "Suède", "Europe");
        add("NO", "Norvège", "Europe");
        add("DK", "Danemark", "Europe");
        add("FI", "Finlande", "Europe");
        add("IS", "Islande", "Europe");
        add("IE", "Irlande", "Europe");
        add("PL", "Pologne", "Europe");
        add("CZ", "République tchèque", "Europe");
        add("SK", "Slovaquie", "Europe");
        add("HU", "Hongrie", "Europe");
        add("RO", "Roumanie", "Europe");
        add("BG", "Bulgarie", "Europe");
        add("HR", "Croatie", "Europe");
        add("SI", "Slovénie", "Europe");
        add("RS", "Serbie", "Europe");
        add("BA", "Bosnie-Herzégovine", "Europe");
        add("ME", "Monténégro", "Europe");
        add("MK", "Macédoine du Nord", "Europe");
        add("AL", "Albanie", "Europe");
        add("GR", "Grèce", "Europe");
        add("CY", "Chypre", "Europe");
        add("MT", "Malte", "Europe");
        add("LU", "Luxembourg", "Europe");
        add("MC", "Monaco", "Europe");
        add("AD", "Andorre", "Europe");
        add("LI", "Liechtenstein", "Europe");
        add("SM", "Saint-Marin", "Europe");
        add("VA", "Vatican", "Europe");
        add("EE", "Estonie", "Europe");
        add("LV", "Lettonie", "Europe");
        add("LT", "Lituanie", "Europe");
        add("BY", "Biélorussie", "Europe");
        add("UA", "Ukraine", "Europe");
        add("MD", "Moldavie", "Europe");
        add("RU", "Russie", "Europe");
        add("TR", "Turquie", "Europe");
        add("GE", "Géorgie", "Europe");
        add("AM", "Arménie", "Europe");
        add("AZ", "Azerbaïdjan", "Europe");
        add("XK", "Kosovo", "Europe");
        // Amérique du Nord
        add("US", "États-Unis", "Amérique du Nord");
        add("CA", "Canada", "Amérique du Nord");
        add("MX", "Mexique", "Amérique du Nord");
        add("GT", "Guatemala", "Amérique du Nord");
        add("BZ", "Belize", "Amérique du Nord");
        add("HN", "Honduras", "Amérique du Nord");
        add("SV", "Salvador", "Amérique du Nord");
        add("NI", "Nicaragua", "Amérique du Nord");
        add("CR", "Costa Rica", "Amérique du Nord");
        add("PA", "Panama", "Amérique du Nord");
        add("CU", "Cuba", "Amérique du Nord");
        add("JM", "Jamaïque", "Amérique du Nord");
        add("HT", "Haïti", "Amérique du Nord");
        add("DO", "République dominicaine", "Amérique du Nord");
        add("PR", "Porto Rico", "Amérique du Nord");
        // Amérique du Sud
        add("BR", "Brésil", "Amérique du Sud");
        add("AR", "Argentine", "Amérique du Sud");
        add("CL", "Chili", "Amérique du Sud");
        add("CO", "Colombie", "Amérique du Sud");
        add("PE", "Pérou", "Amérique du Sud");
        add("VE", "Venezuela", "Amérique du Sud");
        add("EC", "Équateur", "Amérique du Sud");
        add("BO", "Bolivie", "Amérique du Sud");
        add("PY", "Paraguay", "Amérique du Sud");
        add("UY", "Uruguay", "Amérique du Sud");
        add("GY", "Guyana", "Amérique du Sud");
        add("SR", "Suriname", "Amérique du Sud");
        // Afrique
        add("MA", "Maroc", "Afrique");
        add("DZ", "Algérie", "Afrique");
        add("TN", "Tunisie", "Afrique");
        add("LY", "Libye", "Afrique");
        add("EG", "Égypte", "Afrique");
        add("SN", "Sénégal", "Afrique");
        add("ML", "Mali", "Afrique");
        add("MR", "Mauritanie", "Afrique");
        add("BF", "Burkina Faso", "Afrique");
        add("GN", "Guinée", "Afrique");
        add("CI", "Côte d'Ivoire", "Afrique");
        add("GH", "Ghana", "Afrique");
        add("NG", "Nigéria", "Afrique");
        add("NE", "Niger", "Afrique");
        add("TD", "Tchad", "Afrique");
        add("SD", "Soudan", "Afrique");
        add("ET", "Éthiopie", "Afrique");
        add("SO", "Somalie", "Afrique");
        add("KE", "Kenya", "Afrique");
        add("TZ", "Tanzanie", "Afrique");
        add("UG", "Ouganda", "Afrique");
        add("RW", "Rwanda", "Afrique");
        add("BI", "Burundi", "Afrique");
        add("MZ", "Mozambique", "Afrique");
        add("ZA", "Afrique du Sud", "Afrique");
        add("ZW", "Zimbabwe", "Afrique");
        add("ZM", "Zambie", "Afrique");
        add("MG", "Madagascar", "Afrique");
        add("MU", "Maurice", "Afrique");
        add("RE", "La Réunion", "Afrique");
        add("CM", "Cameroun", "Afrique");
        add("GA", "Gabon", "Afrique");
        add("CG", "Congo", "Afrique");
        add("CD", "République démocratique du Congo", "Afrique");
        add("AO", "Angola", "Afrique");
        add("NA", "Namibie", "Afrique");
        add("BW", "Botswana", "Afrique");
        // Asie
        add("CN", "Chine", "Asie");
        add("JP", "Japon", "Asie");
        add("KR", "Corée du Sud", "Asie");
        add("KP", "Corée du Nord", "Asie");
        add("IN", "Inde", "Asie");
        add("PK", "Pakistan", "Asie");
        add("BD", "Bangladesh", "Asie");
        add("LK", "Sri Lanka", "Asie");
        add("NP", "Népal", "Asie");
        add("BT", "Bhoutan", "Asie");
        add("MM", "Myanmar", "Asie");
        add("TH", "Thaïlande", "Asie");
        add("VN", "Vietnam", "Asie");
        add("KH", "Cambodge", "Asie");
        add("LA", "Laos", "Asie");
        add("MY", "Malaisie", "Asie");
        add("SG", "Singapour", "Asie");
        add("ID", "Indonésie", "Asie");
        add("PH", "Philippines", "Asie");
        add("TW", "Taïwan", "Asie");
        add("HK", "Hong Kong", "Asie");
        add("MO", "Macao", "Asie");
        add("MN", "Mongolie", "Asie");
        add("KZ", "Kazakhstan", "Asie");
        add("UZ", "Ouzbékistan", "Asie");
        add("TM", "Turkménistan", "Asie");
        add("TJ", "Tadjikistan", "Asie");
        add("KG", "Kirghizistan", "Asie");
        add("AF", "Afghanistan", "Asie");
        add("IR", "Iran", "Asie");
        add("IQ", "Irak", "Asie");
        add("SY", "Syrie", "Asie");
        add("LB", "Liban", "Asie");
        add("JO", "Jordanie", "Asie");
        add("IL", "Israël", "Asie");
        add("PS", "Palestine", "Asie");
        add("SA", "Arabie Saoudite", "Asie");
        add("AE", "Émirats arabes unis", "Asie");
        add("QA", "Qatar", "Asie");
        add("KW", "Koweït", "Asie");
        add("BH", "Bahreïn", "Asie");
        add("OM", "Oman", "Asie");
        add("YE", "Yémen", "Asie");
        // Océanie
        add("AU", "Australie", "Océanie");
        add("NZ", "Nouvelle-Zélande", "Océanie");
        add("FJ", "Fidji", "Océanie");
        add("PG", "Papouasie-Nouvelle-Guinée", "Océanie");
        add("SB", "Îles Salomon", "Océanie");
        add("VU", "Vanuatu", "Océanie");
        add("WS", "Samoa", "Océanie");
        add("TO", "Tonga", "Océanie");
        add("PF", "Polynésie française", "Océanie");
        add("NC", "Nouvelle-Calédonie", "Océanie");
    }

    private static void add(String iso, String fr, String continent) {
        COUNTRY_DATA.put(iso, new String[]{fr, continent});
    }

    // -------------------------------------------------------------------------
    // Keyword → ISO mapping (French & English names, common aliases)
    // -------------------------------------------------------------------------

    private static final Map<String, String> KEYWORD_TO_ISO = new LinkedHashMap<>();

    static {
        // France
        kw("france", "FR"); kw("français", "FR"); kw("française", "FR");
        // Germany
        kw("allemagne", "DE"); kw("germany", "DE"); kw("berlin", "DE"); kw("munich", "DE"); kw("hamburg", "DE");
        // Spain
        kw("espagne", "ES"); kw("spain", "ES"); kw("madrid", "ES"); kw("barcelone", "ES"); kw("barcelona", "ES");
        kw("séville", "ES"); kw("seville", "ES"); kw("valence", "ES"); kw("valencia", "ES");
        kw("ibiza", "ES"); kw("majorque", "ES"); kw("mallorca", "ES");
        // Italy
        kw("italie", "IT"); kw("italy", "IT"); kw("rome", "IT"); kw("roma", "IT");
        kw("milan", "IT"); kw("milano", "IT"); kw("venise", "IT"); kw("venezia", "IT");
        kw("florence", "IT"); kw("firenze", "IT"); kw("naples", "IT"); kw("napoli", "IT");
        kw("sicile", "IT"); kw("sicilia", "IT"); kw("sardaigne", "IT"); kw("sardegna", "IT");
        // Portugal
        kw("portugal", "PT"); kw("lisbonne", "PT"); kw("lisboa", "PT"); kw("porto", "PT"); kw("algarve", "PT");
        kw("madère", "PT"); kw("madeira", "PT"); kw("açores", "PT"); kw("azores", "PT");
        // UK
        kw("royaume-uni", "GB"); kw("royaume uni", "GB"); kw("angleterre", "GB");
        kw("united kingdom", "GB"); kw("england", "GB"); kw("scotland", "GB"); kw("écosse", "GB");
        kw("london", "GB"); kw("londres", "GB"); kw("manchester", "GB"); kw("edinburgh", "GB");
        // Belgium
        kw("belgique", "BE"); kw("belgium", "BE"); kw("bruxelles", "BE"); kw("brussels", "BE");
        // Netherlands
        kw("pays-bas", "NL"); kw("pays bas", "NL"); kw("netherlands", "NL"); kw("amsterdam", "NL");
        kw("hollande", "NL"); kw("rotterdam", "NL");
        // Switzerland
        kw("suisse", "CH"); kw("switzerland", "CH"); kw("genève", "CH"); kw("zurich", "CH"); kw("bern", "CH");
        // Austria
        kw("autriche", "AT"); kw("austria", "AT"); kw("vienne", "AT"); kw("wien", "AT"); kw("tyrol", "AT");
        // Greece
        kw("grèce", "GR"); kw("greece", "GR"); kw("athènes", "GR"); kw("athenes", "GR");
        kw("santorin", "GR"); kw("mykonos", "GR"); kw("crète", "GR"); kw("crete", "GR");
        // Sweden
        kw("suède", "SE"); kw("sweden", "SE"); kw("stockholm", "SE");
        // Norway
        kw("norvège", "NO"); kw("norway", "NO"); kw("oslo", "NO");
        // Denmark
        kw("danemark", "DK"); kw("denmark", "DK"); kw("copenhague", "DK"); kw("copenhagen", "DK");
        // Finland
        kw("finlande", "FI"); kw("finland", "FI"); kw("helsinki", "FI");
        // Iceland
        kw("islande", "IS"); kw("iceland", "IS"); kw("reykjavik", "IS");
        // Ireland
        kw("irlande", "IE"); kw("ireland", "IE"); kw("dublin", "IE");
        // Poland
        kw("pologne", "PL"); kw("poland", "PL"); kw("varsovie", "PL"); kw("krakow", "PL"); kw("cracovie", "PL");
        // Czech Republic
        kw("république tchèque", "CZ"); kw("czech", "CZ"); kw("prague", "CZ"); kw("tchéquie", "CZ");
        // Hungary
        kw("hongrie", "HU"); kw("hungary", "HU"); kw("budapest", "HU");
        // Romania
        kw("roumanie", "RO"); kw("romania", "RO"); kw("bucarest", "RO");
        // Croatia
        kw("croatie", "HR"); kw("croatia", "HR"); kw("dubrovnik", "HR"); kw("split", "HR");
        // Turkey
        kw("turquie", "TR"); kw("turkey", "TR"); kw("istanbul", "TR"); kw("ankara", "TR"); kw("cappadoce", "TR");
        // Russia
        kw("russie", "RU"); kw("russia", "RU"); kw("moscou", "RU"); kw("moscow", "RU"); kw("saint-pétersbourg", "RU");
        // Ukraine
        kw("ukraine", "UA"); kw("kiev", "UA");
        // USA
        kw("états-unis", "US"); kw("etats-unis", "US"); kw("etats unis", "US"); kw("états unis", "US");
        kw("united states", "US"); kw("usa", "US"); kw("u.s.a", "US"); kw("new york", "US");
        kw("los angeles", "US"); kw("san francisco", "US"); kw("miami", "US"); kw("chicago", "US");
        kw("las vegas", "US"); kw("washington", "US"); kw("boston", "US"); kw("seattle", "US");
        kw("hawaii", "US"); kw("hawaï", "US"); kw("florida", "US"); kw("floride", "US");
        kw("californie", "US"); kw("california", "US"); kw("texas", "US"); kw("nevada", "US");
        kw("arizona", "US"); kw("colorado", "US"); kw("utah", "US"); kw("yellowstone", "US");
        // Canada
        kw("canada", "CA"); kw("toronto", "CA"); kw("montréal", "CA"); kw("montreal", "CA");
        kw("vancouver", "CA"); kw("québec", "CA"); kw("ottawa", "CA");
        // Mexico
        kw("mexique", "MX"); kw("mexico", "MX"); kw("cancún", "MX"); kw("cancun", "MX");
        // Brazil
        kw("brésil", "BR"); kw("brazil", "BR"); kw("rio de janeiro", "BR"); kw("são paulo", "BR"); kw("sao paulo", "BR");
        // Argentina
        kw("argentine", "AR"); kw("argentina", "AR"); kw("buenos aires", "AR"); kw("patagonie", "AR");
        // Peru
        kw("pérou", "PE"); kw("peru", "PE"); kw("lima", "PE"); kw("machu picchu", "PE");
        // Colombia
        kw("colombie", "CO"); kw("colombia", "CO"); kw("bogotá", "CO"); kw("medellin", "CO");
        // Chile
        kw("chili", "CL"); kw("chile", "CL"); kw("santiago", "CL");
        // Morocco
        kw("maroc", "MA"); kw("morocco", "MA"); kw("marrakech", "MA"); kw("casablanca", "MA");
        kw("fès", "MA"); kw("fez", "MA"); kw("agadir", "MA");
        // Tunisia
        kw("tunisie", "TN"); kw("tunisia", "TN"); kw("tunis", "TN"); kw("djerba", "TN");
        // Algeria
        kw("algérie", "DZ"); kw("algeria", "DZ"); kw("alger", "DZ");
        // Egypt
        kw("égypte", "EG"); kw("egypt", "EG"); kw("le caire", "EG"); kw("cairo", "EG"); kw("louxor", "EG");
        kw("louksor", "EG"); kw("hurghada", "EG"); kw("charm el-cheikh", "EG"); kw("sharm el-sheikh", "EG");
        // Senegal
        kw("sénégal", "SN"); kw("dakar", "SN");
        // South Africa
        kw("afrique du sud", "ZA"); kw("south africa", "ZA"); kw("cape town", "ZA"); kw("johannesburg", "ZA");
        // Kenya
        kw("kenya", "KE"); kw("nairobi", "KE");
        // Tanzania
        kw("tanzanie", "TZ"); kw("kilimanjaro", "TZ"); kw("zanzibar", "TZ");
        // Madagascar
        kw("madagascar", "MG");
        // Réunion
        kw("réunion", "RE"); kw("la réunion", "RE");
        // Mauritius
        kw("île maurice", "MU"); kw("mauritius", "MU");
        // China
        kw("chine", "CN"); kw("china", "CN"); kw("beijing", "CN"); kw("pékin", "CN"); kw("shanghai", "CN");
        // Japan
        kw("japon", "JP"); kw("japan", "JP"); kw("tokyo", "JP"); kw("kyoto", "JP"); kw("osaka", "JP");
        // South Korea
        kw("corée du sud", "KR"); kw("south korea", "KR"); kw("séoul", "KR"); kw("seoul", "KR");
        // India
        kw("inde", "IN"); kw("india", "IN"); kw("new delhi", "IN"); kw("delhi", "IN");
        kw("mumbai", "IN"); kw("bombay", "IN"); kw("goa", "IN"); kw("kerala", "IN");
        // Thailand
        kw("thaïlande", "TH"); kw("thailand", "TH"); kw("bangkok", "TH"); kw("phuket", "TH"); kw("chiang mai", "TH");
        // Vietnam
        kw("vietnam", "VN"); kw("hô chi minh", "VN"); kw("ho chi minh", "VN"); kw("hanoi", "VN"); kw("da nang", "VN");
        // Cambodia
        kw("cambodge", "KH"); kw("cambodia", "KH"); kw("angkor", "KH"); kw("phnom penh", "KH");
        // Laos
        kw("laos", "LA"); kw("vientiane", "LA");
        // Malaysia
        kw("malaisie", "MY"); kw("malaysia", "MY"); kw("kuala lumpur", "MY"); kw("borneo", "MY");
        // Singapore
        kw("singapour", "SG"); kw("singapore", "SG");
        // Indonesia
        kw("indonésie", "ID"); kw("indonesia", "ID"); kw("bali", "ID"); kw("jakarta", "ID");
        // Philippines
        kw("philippines", "PH"); kw("manille", "PH"); kw("manila", "PH"); kw("cebu", "PH");
        // Sri Lanka
        kw("sri lanka", "LK"); kw("colombo", "LK");
        // Nepal
        kw("népal", "NP"); kw("nepal", "NP"); kw("kathmandu", "NP"); kw("everest", "NP");
        // United Arab Emirates
        kw("émirats arabes unis", "AE"); kw("émirats", "AE"); kw("uae", "AE");
        kw("dubai", "AE"); kw("dubaï", "AE"); kw("abu dhabi", "AE");
        // Saudi Arabia
        kw("arabie saoudite", "SA"); kw("saudi arabia", "SA"); kw("riyad", "SA");
        // Jordan
        kw("jordanie", "JO"); kw("jordan", "JO"); kw("petra", "JO"); kw("amman", "JO"); kw("wadi rum", "JO");
        // Lebanon
        kw("liban", "LB"); kw("lebanon", "LB"); kw("beyrouth", "LB");
        // Israel
        kw("israël", "IL"); kw("israel", "IL"); kw("tel aviv", "IL"); kw("jérusalem", "IL"); kw("jerusalem", "IL");
        // Iran
        kw("iran", "IR"); kw("téhéran", "IR");
        // Georgia
        kw("géorgie", "GE"); kw("georgia", "GE"); kw("tbilissi", "GE");
        // Australia
        kw("australie", "AU"); kw("australia", "AU"); kw("sydney", "AU"); kw("melbourne", "AU");
        kw("brisbane", "AU"); kw("cairns", "AU");
        // New Zealand
        kw("nouvelle-zélande", "NZ"); kw("new zealand", "NZ"); kw("auckland", "NZ"); kw("wellington", "NZ");
        // French Polynesia
        kw("polynésie française", "PF"); kw("polynésie", "PF"); kw("tahiti", "PF"); kw("bora-bora", "PF"); kw("bora bora", "PF");
        // New Caledonia
        kw("nouvelle-calédonie", "NC"); kw("nouvelle calédonie", "NC"); kw("nouméa", "NC");
        // Guatemala
        kw("guatemala", "GT");
        // Costa Rica
        kw("costa rica", "CR");
        // Cuba
        kw("cuba", "CU"); kw("la havane", "CU"); kw("havana", "CU");
    }

    private static void kw(String keyword, String iso) {
        KEYWORD_TO_ISO.put(keyword.toLowerCase(Locale.ROOT), iso);
    }

    // -------------------------------------------------------------------------
    // France departments
    // -------------------------------------------------------------------------

    private static final Map<String, String> FRANCE_DEPARTMENT_KEYWORDS = new LinkedHashMap<>();

    static {
        // By number
        fd("01", "Ain"); fd("02", "Aisne"); fd("03", "Allier"); fd("04", "Alpes de Haute Provence");
        fd("04", "Alpes-de-Haute-Provence"); fd("05", "Hautes Alpes"); fd("05", "Hautes-Alpes");
        fd("06", "Alpes-Maritimes"); fd("06", "Alpes maritimes"); fd("06", "Nice"); fd("06", "Côte d'Azur");
        fd("07", "Ardèche"); fd("08", "Ardennes"); fd("09", "Ariège"); fd("10", "Aube");
        fd("11", "Aude"); fd("12", "Aveyron"); fd("13", "Bouches-du-Rhône"); fd("13", "Marseille");
        fd("13", "Aix-en-Provence"); fd("14", "Calvados"); fd("14", "Caen"); fd("14", "Normandie");
        fd("15", "Cantal"); fd("16", "Charente"); fd("17", "Charente-Maritime"); fd("17", "La Rochelle");
        fd("18", "Cher"); fd("19", "Corrèze"); fd("2A", "Corse-du-Sud"); fd("2A", "Ajaccio");
        fd("2B", "Haute-Corse"); fd("2B", "Bastia"); fd("21", "Côte-d'Or"); fd("21", "Dijon");
        fd("22", "Côtes-d'Armor"); fd("23", "Creuse"); fd("24", "Dordogne"); fd("24", "Périgord");
        fd("25", "Doubs"); fd("25", "Besançon"); fd("26", "Drôme"); fd("27", "Eure");
        fd("28", "Eure-et-Loir"); fd("28", "Chartres"); fd("29", "Finistère"); fd("29", "Brest");
        fd("29", "Quimper"); fd("30", "Gard"); fd("30", "Nîmes"); fd("31", "Haute-Garonne");
        fd("31", "Toulouse"); fd("32", "Gers"); fd("33", "Gironde"); fd("33", "Bordeaux");
        fd("34", "Hérault"); fd("34", "Montpellier"); fd("35", "Ille-et-Vilaine"); fd("35", "Rennes");
        fd("36", "Indre"); fd("37", "Indre-et-Loire"); fd("37", "Tours"); fd("38", "Isère");
        fd("38", "Grenoble"); fd("39", "Jura"); fd("40", "Landes"); fd("41", "Loir-et-Cher");
        fd("42", "Loire"); fd("42", "Saint-Étienne"); fd("43", "Haute-Loire"); fd("44", "Loire-Atlantique");
        fd("44", "Nantes"); fd("45", "Loiret"); fd("45", "Orléans"); fd("46", "Lot");
        fd("46", "Cahors"); fd("47", "Lot-et-Garonne"); fd("48", "Lozère"); fd("49", "Maine-et-Loire");
        fd("49", "Angers"); fd("50", "Manche"); fd("50", "Cherbourg");
        fd("51", "Marne"); fd("51", "Reims"); fd("52", "Haute-Marne"); fd("53", "Mayenne");
        fd("54", "Meurthe-et-Moselle"); fd("54", "Nancy"); fd("55", "Meuse"); fd("56", "Morbihan");
        fd("56", "Vannes"); fd("56", "Lorient"); fd("57", "Moselle"); fd("57", "Metz");
        fd("58", "Nièvre"); fd("59", "Nord"); fd("59", "Lille"); fd("60", "Oise");
        fd("61", "Orne"); fd("62", "Pas-de-Calais"); fd("62", "Calais"); fd("63", "Puy-de-Dôme");
        fd("63", "Clermont-Ferrand"); fd("64", "Pyrénées-Atlantiques"); fd("64", "Bayonne"); fd("64", "Biarritz");
        fd("64", "Pau"); fd("65", "Hautes-Pyrénées"); fd("65", "Lourdes"); fd("66", "Pyrénées-Orientales");
        fd("66", "Perpignan"); fd("67", "Bas-Rhin"); fd("67", "Strasbourg"); fd("68", "Haut-Rhin");
        fd("68", "Colmar"); fd("68", "Mulhouse"); fd("69", "Rhône"); fd("69", "Lyon");
        fd("70", "Haute-Saône"); fd("71", "Saône-et-Loire"); fd("72", "Sarthe"); fd("72", "Le Mans");
        fd("73", "Savoie"); fd("73", "Chambéry"); fd("74", "Haute-Savoie"); fd("74", "Annecy");
        fd("74", "Chamonix"); fd("75", "Paris"); fd("76", "Seine-Maritime"); fd("76", "Rouen");
        fd("76", "Le Havre"); fd("77", "Seine-et-Marne"); fd("78", "Yvelines"); fd("78", "Versailles");
        fd("79", "Deux-Sèvres"); fd("80", "Somme"); fd("80", "Amiens"); fd("81", "Tarn");
        fd("81", "Albi"); fd("82", "Tarn-et-Garonne"); fd("83", "Var"); fd("83", "Toulon");
        fd("83", "Saint-Tropez"); fd("84", "Vaucluse"); fd("84", "Avignon"); fd("85", "Vendée");
        fd("86", "Vienne"); fd("86", "Poitiers"); fd("87", "Haute-Vienne"); fd("87", "Limoges");
        fd("88", "Vosges"); fd("89", "Yonne"); fd("90", "Territoire de Belfort");
        fd("91", "Essonne"); fd("92", "Hauts-de-Seine"); fd("92", "Levallois"); fd("93", "Seine-Saint-Denis");
        fd("94", "Val-de-Marne"); fd("95", "Val-d'Oise");
        fd("971", "Guadeloupe"); fd("972", "Martinique"); fd("973", "Guyane"); fd("974", "La Réunion");
        fd("976", "Mayotte"); fd("984", "Terres Australes"); fd("987", "Polynésie"); fd("988", "Nouvelle-Calédonie");
        // Alsace historic
        fd("67", "Alsace"); fd("68", "Alsace");
        fd("29", "Bretagne"); fd("35", "Bretagne"); fd("22", "Bretagne"); fd("56", "Bretagne");
        fd("75", "Île-de-France"); fd("77", "Île-de-France"); fd("78", "Île-de-France");
        fd("91", "Île-de-France"); fd("92", "Île-de-France"); fd("93", "Île-de-France");
        fd("94", "Île-de-France"); fd("95", "Île-de-France");
        fd("06", "Provence"); fd("83", "Provence"); fd("84", "Provence"); fd("13", "Provence");
    }

    private static void fd(String num, String name) {
        FRANCE_DEPARTMENT_KEYWORDS.put(name.toLowerCase(Locale.ROOT), num);
    }

    // -------------------------------------------------------------------------
    // France department canonical names
    // -------------------------------------------------------------------------

    private static final Map<String, String> DEPT_NUM_TO_NAME = new LinkedHashMap<>();

    static {
        DEPT_NUM_TO_NAME.put("01", "Ain"); DEPT_NUM_TO_NAME.put("02", "Aisne");
        DEPT_NUM_TO_NAME.put("03", "Allier"); DEPT_NUM_TO_NAME.put("04", "Alpes-de-Haute-Provence");
        DEPT_NUM_TO_NAME.put("05", "Hautes-Alpes"); DEPT_NUM_TO_NAME.put("06", "Alpes-Maritimes");
        DEPT_NUM_TO_NAME.put("07", "Ardèche"); DEPT_NUM_TO_NAME.put("08", "Ardennes");
        DEPT_NUM_TO_NAME.put("09", "Ariège"); DEPT_NUM_TO_NAME.put("10", "Aube");
        DEPT_NUM_TO_NAME.put("11", "Aude"); DEPT_NUM_TO_NAME.put("12", "Aveyron");
        DEPT_NUM_TO_NAME.put("13", "Bouches-du-Rhône"); DEPT_NUM_TO_NAME.put("14", "Calvados");
        DEPT_NUM_TO_NAME.put("15", "Cantal"); DEPT_NUM_TO_NAME.put("16", "Charente");
        DEPT_NUM_TO_NAME.put("17", "Charente-Maritime"); DEPT_NUM_TO_NAME.put("18", "Cher");
        DEPT_NUM_TO_NAME.put("19", "Corrèze"); DEPT_NUM_TO_NAME.put("2A", "Corse-du-Sud");
        DEPT_NUM_TO_NAME.put("2B", "Haute-Corse"); DEPT_NUM_TO_NAME.put("21", "Côte-d'Or");
        DEPT_NUM_TO_NAME.put("22", "Côtes-d'Armor"); DEPT_NUM_TO_NAME.put("23", "Creuse");
        DEPT_NUM_TO_NAME.put("24", "Dordogne"); DEPT_NUM_TO_NAME.put("25", "Doubs");
        DEPT_NUM_TO_NAME.put("26", "Drôme"); DEPT_NUM_TO_NAME.put("27", "Eure");
        DEPT_NUM_TO_NAME.put("28", "Eure-et-Loir"); DEPT_NUM_TO_NAME.put("29", "Finistère");
        DEPT_NUM_TO_NAME.put("30", "Gard"); DEPT_NUM_TO_NAME.put("31", "Haute-Garonne");
        DEPT_NUM_TO_NAME.put("32", "Gers"); DEPT_NUM_TO_NAME.put("33", "Gironde");
        DEPT_NUM_TO_NAME.put("34", "Hérault"); DEPT_NUM_TO_NAME.put("35", "Ille-et-Vilaine");
        DEPT_NUM_TO_NAME.put("36", "Indre"); DEPT_NUM_TO_NAME.put("37", "Indre-et-Loire");
        DEPT_NUM_TO_NAME.put("38", "Isère"); DEPT_NUM_TO_NAME.put("39", "Jura");
        DEPT_NUM_TO_NAME.put("40", "Landes"); DEPT_NUM_TO_NAME.put("41", "Loir-et-Cher");
        DEPT_NUM_TO_NAME.put("42", "Loire"); DEPT_NUM_TO_NAME.put("43", "Haute-Loire");
        DEPT_NUM_TO_NAME.put("44", "Loire-Atlantique"); DEPT_NUM_TO_NAME.put("45", "Loiret");
        DEPT_NUM_TO_NAME.put("46", "Lot"); DEPT_NUM_TO_NAME.put("47", "Lot-et-Garonne");
        DEPT_NUM_TO_NAME.put("48", "Lozère"); DEPT_NUM_TO_NAME.put("49", "Maine-et-Loire");
        DEPT_NUM_TO_NAME.put("50", "Manche"); DEPT_NUM_TO_NAME.put("51", "Marne");
        DEPT_NUM_TO_NAME.put("52", "Haute-Marne"); DEPT_NUM_TO_NAME.put("53", "Mayenne");
        DEPT_NUM_TO_NAME.put("54", "Meurthe-et-Moselle"); DEPT_NUM_TO_NAME.put("55", "Meuse");
        DEPT_NUM_TO_NAME.put("56", "Morbihan"); DEPT_NUM_TO_NAME.put("57", "Moselle");
        DEPT_NUM_TO_NAME.put("58", "Nièvre"); DEPT_NUM_TO_NAME.put("59", "Nord");
        DEPT_NUM_TO_NAME.put("60", "Oise"); DEPT_NUM_TO_NAME.put("61", "Orne");
        DEPT_NUM_TO_NAME.put("62", "Pas-de-Calais"); DEPT_NUM_TO_NAME.put("63", "Puy-de-Dôme");
        DEPT_NUM_TO_NAME.put("64", "Pyrénées-Atlantiques"); DEPT_NUM_TO_NAME.put("65", "Hautes-Pyrénées");
        DEPT_NUM_TO_NAME.put("66", "Pyrénées-Orientales"); DEPT_NUM_TO_NAME.put("67", "Bas-Rhin");
        DEPT_NUM_TO_NAME.put("68", "Haut-Rhin"); DEPT_NUM_TO_NAME.put("69", "Rhône");
        DEPT_NUM_TO_NAME.put("70", "Haute-Saône"); DEPT_NUM_TO_NAME.put("71", "Saône-et-Loire");
        DEPT_NUM_TO_NAME.put("72", "Sarthe"); DEPT_NUM_TO_NAME.put("73", "Savoie");
        DEPT_NUM_TO_NAME.put("74", "Haute-Savoie"); DEPT_NUM_TO_NAME.put("75", "Paris");
        DEPT_NUM_TO_NAME.put("76", "Seine-Maritime"); DEPT_NUM_TO_NAME.put("77", "Seine-et-Marne");
        DEPT_NUM_TO_NAME.put("78", "Yvelines"); DEPT_NUM_TO_NAME.put("79", "Deux-Sèvres");
        DEPT_NUM_TO_NAME.put("80", "Somme"); DEPT_NUM_TO_NAME.put("81", "Tarn");
        DEPT_NUM_TO_NAME.put("82", "Tarn-et-Garonne"); DEPT_NUM_TO_NAME.put("83", "Var");
        DEPT_NUM_TO_NAME.put("84", "Vaucluse"); DEPT_NUM_TO_NAME.put("85", "Vendée");
        DEPT_NUM_TO_NAME.put("86", "Vienne"); DEPT_NUM_TO_NAME.put("87", "Haute-Vienne");
        DEPT_NUM_TO_NAME.put("88", "Vosges"); DEPT_NUM_TO_NAME.put("89", "Yonne");
        DEPT_NUM_TO_NAME.put("90", "Territoire de Belfort"); DEPT_NUM_TO_NAME.put("91", "Essonne");
        DEPT_NUM_TO_NAME.put("92", "Hauts-de-Seine"); DEPT_NUM_TO_NAME.put("93", "Seine-Saint-Denis");
        DEPT_NUM_TO_NAME.put("94", "Val-de-Marne"); DEPT_NUM_TO_NAME.put("95", "Val-d'Oise");
        DEPT_NUM_TO_NAME.put("971", "Guadeloupe"); DEPT_NUM_TO_NAME.put("972", "Martinique");
        DEPT_NUM_TO_NAME.put("973", "Guyane"); DEPT_NUM_TO_NAME.put("974", "La Réunion");
        DEPT_NUM_TO_NAME.put("976", "Mayotte");
    }

    // -------------------------------------------------------------------------
    // USA States
    // -------------------------------------------------------------------------

    private static final Map<String, String> USA_STATE_KEYWORDS = new LinkedHashMap<>();
    private static final Map<String, String> USA_STATE_NAMES = new LinkedHashMap<>();

    static {
        us("AL", "Alabama"); us("AK", "Alaska"); us("AZ", "Arizona"); us("AR", "Arkansas");
        us("CA", "California"); us("CA", "Californie"); us("CO", "Colorado"); us("CT", "Connecticut");
        us("DE", "Delaware"); us("FL", "Florida"); us("FL", "Floride"); us("GA", "Georgia (US)");
        us("HI", "Hawaii"); us("HI", "Hawaï"); us("ID", "Idaho"); us("IL", "Illinois");
        us("IL", "Chicago"); us("IN", "Indiana"); us("IA", "Iowa"); us("KS", "Kansas");
        us("KY", "Kentucky"); us("LA", "Louisiana"); us("LA", "Louisiane");
        us("ME", "Maine (US)"); us("MD", "Maryland"); us("MA", "Massachusetts"); us("MA", "Boston");
        us("MI", "Michigan"); us("MN", "Minnesota"); us("MS", "Mississippi"); us("MO", "Missouri");
        us("MT", "Montana"); us("NE", "Nebraska"); us("NV", "Nevada"); us("NV", "Las Vegas");
        us("NH", "New Hampshire"); us("NJ", "New Jersey"); us("NM", "New Mexico");
        us("NY", "New York"); us("NY", "Manhattan"); us("NY", "Brooklyn");
        us("NC", "North Carolina"); us("ND", "North Dakota"); us("OH", "Ohio");
        us("OK", "Oklahoma"); us("OR", "Oregon"); us("PA", "Pennsylvania");
        us("RI", "Rhode Island"); us("SC", "South Carolina"); us("SD", "South Dakota");
        us("TN", "Tennessee"); us("TN", "Nashville"); us("TX", "Texas"); us("TX", "Dallas");
        us("TX", "Houston"); us("TX", "Austin"); us("UT", "Utah");
        us("VT", "Vermont"); us("VA", "Virginia"); us("WA", "Washington (State)");
        us("WA", "Seattle"); us("WA", "Washington State");
        us("WV", "West Virginia"); us("WI", "Wisconsin"); us("WY", "Wyoming");
        us("DC", "Washington DC"); us("DC", "Washington D.C.");
        us("CA", "San Francisco"); us("CA", "Los Angeles"); us("CA", "LA"); us("CA", "San Diego");
        us("FL", "Miami"); us("FL", "Orlando"); us("FL", "Tampa");
        us("NY", "New York City"); us("NY", "NYC");
        us("AZ", "Phoenix"); us("AZ", "Scottsdale"); us("AZ", "Sedona"); us("AZ", "Grand Canyon");
        us("CO", "Denver"); us("CO", "Aspen"); us("CO", "Vail");
        us("UT", "Salt Lake City"); us("UT", "Moab"); us("UT", "Zion");
        us("WY", "Yellowstone");
    }

    private static void us(String code, String name) {
        String canonical = USA_STATE_NAMES.computeIfAbsent(code, k -> name);
        // keep first registration as canonical name
        if (!USA_STATE_NAMES.containsKey(code)) {
            USA_STATE_NAMES.put(code, name);
        }
        USA_STATE_KEYWORDS.put(name.toLowerCase(Locale.ROOT), code);
        // Also register the 2-letter code itself as a word (boundary match done at parse time)
        USA_STATE_KEYWORDS.put(code.toLowerCase(Locale.ROOT), code);
    }

    // canonical state names
    private static final Map<String, String> USA_STATE_CANONICAL = new LinkedHashMap<>();

    static {
        USA_STATE_CANONICAL.put("AL", "Alabama"); USA_STATE_CANONICAL.put("AK", "Alaska");
        USA_STATE_CANONICAL.put("AZ", "Arizona"); USA_STATE_CANONICAL.put("AR", "Arkansas");
        USA_STATE_CANONICAL.put("CA", "Californie"); USA_STATE_CANONICAL.put("CO", "Colorado");
        USA_STATE_CANONICAL.put("CT", "Connecticut"); USA_STATE_CANONICAL.put("DE", "Delaware");
        USA_STATE_CANONICAL.put("FL", "Floride"); USA_STATE_CANONICAL.put("GA", "Géorgie (US)");
        USA_STATE_CANONICAL.put("HI", "Hawaï"); USA_STATE_CANONICAL.put("ID", "Idaho");
        USA_STATE_CANONICAL.put("IL", "Illinois"); USA_STATE_CANONICAL.put("IN", "Indiana");
        USA_STATE_CANONICAL.put("IA", "Iowa"); USA_STATE_CANONICAL.put("KS", "Kansas");
        USA_STATE_CANONICAL.put("KY", "Kentucky"); USA_STATE_CANONICAL.put("LA", "Louisiane");
        USA_STATE_CANONICAL.put("ME", "Maine"); USA_STATE_CANONICAL.put("MD", "Maryland");
        USA_STATE_CANONICAL.put("MA", "Massachusetts"); USA_STATE_CANONICAL.put("MI", "Michigan");
        USA_STATE_CANONICAL.put("MN", "Minnesota"); USA_STATE_CANONICAL.put("MS", "Mississippi");
        USA_STATE_CANONICAL.put("MO", "Missouri"); USA_STATE_CANONICAL.put("MT", "Montana");
        USA_STATE_CANONICAL.put("NE", "Nebraska"); USA_STATE_CANONICAL.put("NV", "Nevada");
        USA_STATE_CANONICAL.put("NH", "New Hampshire"); USA_STATE_CANONICAL.put("NJ", "New Jersey");
        USA_STATE_CANONICAL.put("NM", "Nouveau-Mexique"); USA_STATE_CANONICAL.put("NY", "New York");
        USA_STATE_CANONICAL.put("NC", "Caroline du Nord"); USA_STATE_CANONICAL.put("ND", "Dakota du Nord");
        USA_STATE_CANONICAL.put("OH", "Ohio"); USA_STATE_CANONICAL.put("OK", "Oklahoma");
        USA_STATE_CANONICAL.put("OR", "Oregon"); USA_STATE_CANONICAL.put("PA", "Pennsylvanie");
        USA_STATE_CANONICAL.put("RI", "Rhode Island"); USA_STATE_CANONICAL.put("SC", "Caroline du Sud");
        USA_STATE_CANONICAL.put("SD", "Dakota du Sud"); USA_STATE_CANONICAL.put("TN", "Tennessee");
        USA_STATE_CANONICAL.put("TX", "Texas"); USA_STATE_CANONICAL.put("UT", "Utah");
        USA_STATE_CANONICAL.put("VT", "Vermont"); USA_STATE_CANONICAL.put("VA", "Virginie");
        USA_STATE_CANONICAL.put("WA", "Washington"); USA_STATE_CANONICAL.put("WV", "Virginie-Occidentale");
        USA_STATE_CANONICAL.put("WI", "Wisconsin"); USA_STATE_CANONICAL.put("WY", "Wyoming");
        USA_STATE_CANONICAL.put("DC", "Washington D.C.");
    }

    // -------------------------------------------------------------------------
    // Public API
    // -------------------------------------------------------------------------

    /**
     * Parse a free-text location string and return structured info.
     * Returns null if the country cannot be determined.
     */
    public LocationInfo parse(String location) {
        if (location == null || location.isBlank()) return null;
        String normalized = location.toLowerCase(Locale.ROOT).trim();

        // 1. Try to find country keyword (longest match first for accuracy)
        String isoCode = null;
        int matchLen = 0;
        for (Map.Entry<String, String> entry : KEYWORD_TO_ISO.entrySet()) {
            String kw = entry.getKey();
            if (normalized.contains(kw) && kw.length() > matchLen) {
                isoCode = entry.getValue();
                matchLen = kw.length();
            }
        }
        if (isoCode == null) return null;

        String[] data = COUNTRY_DATA.get(isoCode);
        if (data == null) return null;

        String countryFr = data[0];
        String continent = data[1];
        String flag = buildFlag(isoCode);
        String subdivision = null;

        // 2. For France, detect department
        if ("FR".equals(isoCode)) {
            subdivision = detectFranceDepartment(normalized);
        }
        // 3. For USA, detect state
        else if ("US".equals(isoCode)) {
            subdivision = detectUsaState(normalized);
        }

        return new LocationInfo(isoCode, countryFr, continent, flag, subdivision);
    }

    private String detectFranceDepartment(String normalized) {
        // Check for explicit department number pattern like "(75)" or "75 -" etc.
        java.util.regex.Matcher m = java.util.regex.Pattern
                .compile("\\b(9[7][1-6]|[0-9]{2}|2[ab])\\b").matcher(normalized);
        while (m.find()) {
            String num = m.group(1).toUpperCase();
            if (DEPT_NUM_TO_NAME.containsKey(num)) {
                return DEPT_NUM_TO_NAME.get(num);
            }
        }
        // Otherwise try keyword match (longest first)
        String best = null;
        int bestLen = 0;
        for (Map.Entry<String, String> entry : FRANCE_DEPARTMENT_KEYWORDS.entrySet()) {
            String kw = entry.getKey();
            if (normalized.contains(kw) && kw.length() > bestLen) {
                String deptNum = entry.getValue();
                String canonical = DEPT_NUM_TO_NAME.get(deptNum);
                if (canonical != null) {
                    best = canonical;
                    bestLen = kw.length();
                }
            }
        }
        return best;
    }

    private String detectUsaState(String normalized) {
        String best = null;
        int bestLen = 0;
        for (Map.Entry<String, String> entry : USA_STATE_KEYWORDS.entrySet()) {
            String kw = entry.getKey();
            if (kw.length() <= 2) {
                // Short codes require word boundary
                if (java.util.regex.Pattern.compile("\\b" + java.util.regex.Pattern.quote(kw) + "\\b")
                        .matcher(normalized).find() && kw.length() > bestLen) {
                    String stateCode = entry.getValue();
                    best = USA_STATE_CANONICAL.getOrDefault(stateCode, stateCode);
                    bestLen = kw.length();
                }
            } else {
                if (normalized.contains(kw) && kw.length() > bestLen) {
                    String stateCode = entry.getValue();
                    best = USA_STATE_CANONICAL.getOrDefault(stateCode, stateCode);
                    bestLen = kw.length();
                }
            }
        }
        return best;
    }

    /**
     * Build a flag emoji from a 2-letter ISO country code.
     */
    public static String buildFlag(String isoCode) {
        if (isoCode == null || isoCode.length() != 2) return "🏳";
        int[] codePoints = isoCode.toUpperCase(Locale.ROOT).chars()
                .map(c -> c - 'A' + 0x1F1E6)
                .toArray();
        return new String(codePoints, 0, codePoints.length);
    }

    /**
     * Build a LocationInfo directly from an ISO code (no subdivision).
     * Returns null if the ISO code is unknown.
     */
    public LocationInfo lookupByIso(String isoCode) {
        if (isoCode == null) return null;
        String[] data = COUNTRY_DATA.get(isoCode.toUpperCase(Locale.ROOT));
        if (data == null) return null;
        return new LocationInfo(isoCode.toUpperCase(Locale.ROOT), data[0], data[1],
                buildFlag(isoCode), null);
    }

    // Expose for iteration in WorldStatsService
    public Map<String, String[]> getCountryData() {
        return COUNTRY_DATA;
    }
}
