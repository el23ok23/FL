package com.fletaba.app.data

import java.text.Normalizer

/**
 * Calles y avenidas principales de Capital Federal para autocompletar el ORIGEN sin internet
 * ni clave de Google: al escribir "corr" aparece "Av. Corrientes", se completa y solo queda
 * agregar la altura. La geocodificación final la hace el servicio de direcciones.
 */
object CabaStreets {
    val names: List<String> = listOf(
        "Av. 9 de Julio", "Av. Corrientes", "Av. Rivadavia", "Av. Santa Fe", "Av. Córdoba", "Av. Callao",
        "Av. Belgrano", "Av. Independencia", "Av. San Juan", "Av. Caseros", "Av. Entre Ríos", "Av. Jujuy",
        "Av. Pueyrredón", "Av. Las Heras", "Av. del Libertador", "Av. Figueroa Alcorta", "Av. Alvear",
        "Av. Cabildo", "Av. Juramento", "Av. Monroe", "Av. Congreso", "Av. Olazábal", "Av. Crisólogo Larralde",
        "Av. García del Río", "Av. Ruiz Huidobro", "Av. Balbín", "Av. de los Constituyentes", "Av. de los Incas",
        "Av. Triunvirato", "Av. Álvarez Thomas", "Av. Elcano", "Av. Federico Lacroze", "Av. Dorrego",
        "Av. Forest", "Av. Chorroarín", "Av. San Martín", "Av. Nazca", "Av. Gaona", "Av. Juan B. Justo",
        "Av. Warnes", "Av. Scalabrini Ortiz", "Av. Raúl Scalabrini Ortiz", "Av. Coronel Díaz", "Av. Medrano",
        "Av. Díaz Vélez", "Av. Ángel Gallardo", "Av. Honorio Pueyrredón", "Av. Acoyte", "Av. La Plata",
        "Av. Boedo", "Av. Chiclana", "Av. Sáenz", "Av. Amancio Alcorta", "Av. Vélez Sarsfield", "Av. Montes de Oca",
        "Av. Regimiento de Patricios", "Av. Almirante Brown", "Av. Pedro de Mendoza", "Av. Paseo Colón",
        "Av. Leandro N. Alem", "Av. Eduardo Madero", "Av. Antártida Argentina", "Av. de Mayo", "Av. Roque Sáenz Peña",
        "Av. Julio A. Roca", "Av. Presidente Roque Sáenz Peña", "Av. Córdoba", "Av. Directorio", "Av. Eva Perón",
        "Av. Juan Bautista Alberdi", "Av. Rivadavia", "Av. Carabobo", "Av. Varela", "Av. Castañares", "Av. Perito Moreno",
        "Av. Roca", "Av. Lacarra", "Av. Escalada", "Av. Dellepiane", "Av. General Paz", "Av. Emilio Castro",
        "Av. Juan Bautista Alberdi", "Av. Álvarez Jonte", "Av. Segurola", "Av. Lope de Vega", "Av. Francisco Beiró",
        "Av. Mosconi", "Av. Nazca", "Av. Cuenca", "Av. Jonte", "Av. Salvador María del Carril", "Av. de los Constituyentes",
        "Av. Lincoln", "Av. Tronador", "Av. Combatientes de Malvinas", "Av. Pedro Goyena", "Av. José María Moreno",
        "Av. Asamblea", "Av. Hipólito Yrigoyen", "Av. Caseros", "Av. Garay", "Av. Brasil", "Av. Juan de Garay",
        "Av. Martín García", "Av. Suárez", "Av. Iriarte", "Av. Osvaldo Cruz", "Av. Castro Barros", "Av. Colonia",
        "Av. Costanera Rafael Obligado", "Av. Sarmiento", "Av. Infanta Isabel", "Av. Bullrich", "Av. Luis María Campos",
        "Av. Ortiz de Ocampo", "Av. Presidente Quintana", "Av. Casares", "Av. Udaondo", "Av. Lugones", "Av. Cantilo",
        "Av. Guzmán", "Av. Rolón", "Av. Chivilcoy", "Av. Rivadavia", "Av. Avellaneda", "Av. Emilio Mitre",
        "Av. Curapaligüe", "Av. Cobo", "Av. Fernández de la Cruz", "Av. Riestra", "Av. Mariano Acosta", "Av. Larrazábal",
        "Av. Piedra Buena", "Av. Coronel Roca", "Av. 27 de Febrero", "Av. Intendente Cantilo",
        "Florida", "Lavalle", "Sarmiento", "Perón", "Bartolomé Mitre", "Tucumán", "Viamonte", "Paraguay", "Marcelo T. de Alvear",
        "Arenales", "Juncal", "Guido", "Posadas", "Esmeralda", "Suipacha", "Maipú", "San Martín", "Reconquista",
        "25 de Mayo", "Cerrito", "Carlos Pellegrini", "Libertad", "Talcahuano", "Uruguay", "Paraná", "Montevideo",
        "Rodríguez Peña", "Ayacucho", "Riobamba", "Azcuénaga", "Larrea", "Paso", "Pasteur", "Junín", "Uriburu",
        "Ecuador", "Anchorena", "Jean Jaurés", "Laprida", "Agüero", "Gallo", "Bustamante", "Billinghurst", "Sánchez de Bustamante",
        "Salguero", "Bulnes", "Mario Bravo", "Sánchez de Loria", "Castro Barros", "Yatay", "Lambaré", "Palestina",
        "Lima", "Salta", "Santiago del Estero", "San José", "Luis Sáenz Peña", "Virrey Cevallos", "Solís", "Combate de los Pozos",
        "Sarandí", "Rincón", "Pasco", "Pichincha", "Matheu", "Alberti", "Saavedra", "La Rioja", "Deán Funes", "Catamarca",
        "Urquiza", "Esparza", "Treinta y Tres Orientales", "Muñiz", "Quintino Bocayuva", "Colombres", "Maza", "Boedo",
        "Defensa", "Bolívar", "Perú", "Chacabuco", "Piedras", "Tacuarí", "Bernardo de Irigoyen", "Alsina", "Moreno",
        "Venezuela", "México", "Chile", "Estados Unidos", "Carlos Calvo", "Humberto Primo", "Cochabamba", "Brasil",
        "Balcarce", "Azopardo", "Ingeniero Huergo", "Juana Manso", "Olga Cossettini", "Alicia Moreau de Justo", "Pierina Dealessi",
        "Thames", "Gurruchaga", "Armenia", "Malabia", "Serrano", "Borges", "Fitz Roy", "Humboldt", "Bonpland", "Arévalo",
        "Ravignani", "Carranza", "Gorriti", "Honduras", "El Salvador", "Costa Rica", "Nicaragua", "Soler", "Guatemala",
        "Charcas", "Güemes", "Cabrera", "Niceto Vega", "Loyola", "Aguirre", "Padilla", "Castillo", "Vera", "Velasco",
        "Jufré", "Lerma", "Camargo", "Ramírez de Velasco", "Darwin", "Newbery", "Zapiola", "Conde", "Vidal", "Amenábar",
        "Ciudad de la Paz", "Moldes", "Cramer", "Conesa", "Freire", "Vuelta de Obligado", "Arcos", "O'Higgins", "Montañeses",
        "Cuba", "Echeverría", "Sucre", "Virrey del Pino", "La Pampa", "Mendoza", "Blanco Encalada", "Zabala", "Virrey Loreto",
        "Olleros", "Maure", "Teodoro García", "Matienzo", "Olleros", "Gral. Enrique Martínez", "Delgado", "Gral. Ramón Freire",
        "Mariscal Antonio José de Sucre", "Juana Azurduy", "Roosevelt", "Quesada", "Núñez", "Manuela Pedraza", "Ramallo",
        "Pico", "Vedia", "Deheza", "Besares", "Arias", "Correa", "Carbajal", "Ceretti", "Bauness", "Mariano Acha",
        "Estomba", "Burela", "Andonaegui", "Donado", "Holmberg", "Melián", "Washington", "Plaza", "Franklin", "Roseti",
        "Galván", "Zamudio", "Bolivia", "Argerich", "Terrada", "Bermúdez", "Joaquín V. González", "Gavilán", "Caracas",
        "Boyacá", "Añasco", "Espinosa", "Nicasio Oroño", "Franklin", "Yerbal", "Bogotá", "Neuquén", "Formosa", "Bacacay",
        "Morón", "Aranguren", "Emilio Mitre", "Puán", "Pedro Goyena", "Valle", "Senillosa", "Beauchef", "Del Barco Centenera",
        "Doblas", "Pumacahua", "Miró", "Cachimayo", "Thorne", "Membrillar", "Hortiguera", "Yerbal", "Terrero", "Bonifacio",
        "Zuviría", "Zelarrayán", "Tandil", "Avelino Díaz", "Zañartú", "Cnel. Ramón L. Falcón", "Ramón Falcón", "Bilbao",
        "Miralla", "Murguiondo", "Guaminí", "Pola", "Cañada de Gómez", "Tapalqué", "Timoteo Gordillo", "Montiel",
        "Vernet", "Pergamino", "Mom", "Laguna", "Pepirí", "Traful", "Ancaste", "Río Cuarto", "Luna", "Pilar",
        "Olavarría", "Lamadrid", "Necochea", "Pinzón", "Wenceslao Villafañe", "Aristóbulo del Valle", "Benito Pérez Galdós",
        "Pedro de Mendoza", "Caminito", "Magallanes", "Palos", "Blanes", "Alvarado", "Aráoz", "Gascón", "Lavalleja",
        "Julián Álvarez", "Jerónimo Salguero", "Vidt", "Oro", "Fray Justo Santa María de Oro", "Ugarteche", "Cerviño",
        "Seguí", "Demaría", "Pacheco de Melo", "Peña", "French", "Beruti", "Agote", "Gelly y Obes", "Schiaffino", "Ayacucho",
    ).distinct()

    private fun norm(s: String): String =
        Normalizer.normalize(s, Normalizer.Form.NFD).replace(Regex("\\p{Mn}+"), "").lowercase().trim()

    /** Hasta [max] calles que empiezan por (o contienen) lo escrito, cuando todavía no hay altura. */
    fun suggest(query: String, max: Int = 6): List<String> {
        val q = norm(query.removePrefix("av.").removePrefix("av").trim())
        if (q.length < 2 || query.any { it.isDigit() }) return emptyList()
        val starts = names.filter { n -> norm(n).removePrefix("av. ").startsWith(q) || norm(n).startsWith(q) }
        val contains = names.filter { n -> n !in starts && norm(n).contains(q) }
        return (starts + contains).take(max)
    }
}
