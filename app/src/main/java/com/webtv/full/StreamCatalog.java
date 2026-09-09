package com.webtv.full;

import java.util.ArrayList;
import java.util.List;

/**
 * Catálogo nativo do app.
 *
 * IMPORTANTE: substitua somente as strings URL pelos endpoints de mídia autorizados
 * (por exemplo, HLS .m3u8 ou MP4) fornecidos pelo responsável pelos sinais.
 */
public final class StreamCatalog {
    private StreamCatalog() {}

    public static List<Content> items() {
        List<Content> list = new ArrayList<>();

        // ESPORTES — fonte de jogos/esportes
        list.add(new Content("Jogo ao vivo 1", "ESPORTES", "AO VIVO", ""));
        list.add(new Content("Jogo ao vivo 2", "ESPORTES", "AO VIVO", ""));
        list.add(new Content("Esportes ao vivo", "ESPORTES", "AO VIVO", ""));

        // NOVELAS — catálogo nativo
        list.add(new Content("Novela 1", "NOVELAS", "NOVELA", ""));
        list.add(new Content("Novela 2", "NOVELAS", "NOVELA", ""));
        list.add(new Content("Novela 3", "NOVELAS", "NOVELA", ""));
        list.add(new Content("Novela 4", "NOVELAS", "NOVELA", ""));

        // CANAIS — TV ao vivo
        list.add(new Content("Canal 1", "CANAIS", "AO VIVO", ""));
        list.add(new Content("Canal 2", "CANAIS", "AO VIVO", ""));
        list.add(new Content("Canal 3", "CANAIS", "AO VIVO", ""));
        list.add(new Content("Canal de notícias", "CANAIS", "AO VIVO", ""));

        return list;
    }
}
