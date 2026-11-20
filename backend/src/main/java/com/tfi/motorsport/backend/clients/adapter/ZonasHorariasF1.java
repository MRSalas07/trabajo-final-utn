package com.tfi.motorsport.backend.clients.adapter;


import java.util.Map;

/**
 * Jolpica informa la ubicación del circuito (país, ciudad, lat/long) pero NO su zona horaria.
 * La zona IANA de la sede se necesita para mostrar "hora local del circuito" y para el .ics.
 *
 * Se resuelve con una tabla estática por circuitId (los circuitos cambian poco) y,
 * si el circuito no está, con una tabla por país. Último recurso: "UTC".
 */
final class ZonasHorariasF1 {

    private ZonasHorariasF1() {
    }

    private static final Map<String, String> POR_CIRCUITO = Map.ofEntries(
            Map.entry("albert_park", "Australia/Melbourne"),
            Map.entry("shanghai", "Asia/Shanghai"),
            Map.entry("suzuka", "Asia/Tokyo"),
            Map.entry("bahrain", "Asia/Bahrain"),
            Map.entry("jeddah", "Asia/Riyadh"),
            Map.entry("miami", "America/New_York"),
            Map.entry("imola", "Europe/Rome"),
            Map.entry("monaco", "Europe/Monaco"),
            Map.entry("catalunya", "Europe/Madrid"),
            Map.entry("madring", "Europe/Madrid"),
            Map.entry("villeneuve", "America/Toronto"),
            Map.entry("red_bull_ring", "Europe/Vienna"),
            Map.entry("silverstone", "Europe/London"),
            Map.entry("spa", "Europe/Brussels"),
            Map.entry("hungaroring", "Europe/Budapest"),
            Map.entry("zandvoort", "Europe/Amsterdam"),
            Map.entry("monza", "Europe/Rome"),
            Map.entry("baku", "Asia/Baku"),
            Map.entry("marina_bay", "Asia/Singapore"),
            Map.entry("americas", "America/Chicago"),
            Map.entry("rodriguez", "America/Mexico_City"),
            Map.entry("interlagos", "America/Sao_Paulo"),
            Map.entry("vegas", "America/Los_Angeles"),
            Map.entry("losail", "Asia/Qatar"),
            Map.entry("yas_marina", "Asia/Dubai"),
            Map.entry("ricard", "Europe/Paris"),
            Map.entry("portimao", "Europe/Lisbon"),
            Map.entry("istanbul", "Europe/Istanbul"),
            Map.entry("sochi", "Europe/Moscow"),
            Map.entry("nurburgring", "Europe/Berlin"),
            Map.entry("hockenheimring", "Europe/Berlin"),
            Map.entry("mugello", "Europe/Rome"),
            Map.entry("sepang", "Asia/Kuala_Lumpur"),
            Map.entry("galvez", "America/Argentina/Buenos_Aires")
    );

    private static final Map<String, String> POR_PAIS = Map.ofEntries(
            Map.entry("Australia", "Australia/Melbourne"),
            Map.entry("China", "Asia/Shanghai"),
            Map.entry("Japan", "Asia/Tokyo"),
            Map.entry("Bahrain", "Asia/Bahrain"),
            Map.entry("Saudi Arabia", "Asia/Riyadh"),
            Map.entry("USA", "America/New_York"),
            Map.entry("United States", "America/New_York"),
            Map.entry("Italy", "Europe/Rome"),
            Map.entry("Monaco", "Europe/Monaco"),
            Map.entry("Spain", "Europe/Madrid"),
            Map.entry("Canada", "America/Toronto"),
            Map.entry("Austria", "Europe/Vienna"),
            Map.entry("UK", "Europe/London"),
            Map.entry("Belgium", "Europe/Brussels"),
            Map.entry("Hungary", "Europe/Budapest"),
            Map.entry("Netherlands", "Europe/Amsterdam"),
            Map.entry("Azerbaijan", "Asia/Baku"),
            Map.entry("Singapore", "Asia/Singapore"),
            Map.entry("Mexico", "America/Mexico_City"),
            Map.entry("Brazil", "America/Sao_Paulo"),
            Map.entry("Qatar", "Asia/Qatar"),
            Map.entry("UAE", "Asia/Dubai"),
            Map.entry("France", "Europe/Paris"),
            Map.entry("Portugal", "Europe/Lisbon"),
            Map.entry("Turkey", "Europe/Istanbul"),
            Map.entry("Germany", "Europe/Berlin"),
            Map.entry("Argentina", "America/Argentina/Buenos_Aires")
    );

    static String resolver(String circuitId, String pais) {
        if (circuitId != null && POR_CIRCUITO.containsKey(circuitId)) {
            return POR_CIRCUITO.get(circuitId);
        }
        if (pais != null && POR_PAIS.containsKey(pais)) {
            return POR_PAIS.get(pais);
        }
        return "UTC";
    }
}
