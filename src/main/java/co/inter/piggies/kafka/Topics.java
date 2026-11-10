package co.inter.piggies.kafka;

/*
 *----------------------------------------------------------------------*
 * Classe que centraliza os nomes dos tópicos do Kafka.                 *
 * Usar constantes evita erro de digitação: producer e consumer apontam *
 * para a mesma constante, e se o nome mudar, muda em um lugar só.      *
 *----------------------------------------------------------------------*
 * final + construtor privado: a classe só guarda constantes, então não *
 * pode ser herdada nem instanciada.                                    *
 *----------------------------------------------------------------------*
 */
public final class Topics {

    private Topics() {
    }

    /*
     *-----------------------------------------------*
     * Nome do tópico. Convenção: tudo em minúsculo, *
     * separado por hífen (ex.: produto-criado).     *
     *-----------------------------------------------*
     */
    public static final String APPOINTMENTCREATED = "appointment.created";
}
