Web Rádio Vem Comigo no Glória - versão Caster.fm Free + Hora Certa

Esta versão usa o player oficial/embutido do Caster.fm com o public token da estação.
O plano Free do Caster.fm não libera o Direct Stream Link; a documentação oficial diz que no Free o caminho suportado é o player embutido. O app, portanto, não inventa um stream.

Hora Certa:
- usa os arquivos ZaraRadio HRS00_O.mp3 ... HRS23_O.mp3 incluídos em res/raw;
- verifica o relógio a cada segundo;
- na hora cheia, tenta pausar o áudio HTML5 do player, toca a Hora Certa local e depois tenta retomar o player.

Importante: a capacidade de pausar/retomar o áudio do widget depende de como o Caster.fm renderiza o player no dispositivo. Se o widget colocar o áudio em um contexto isolado, o stream direto (disponível no Cloud Plus) será necessário para uma integração nativa 100% determinística.
