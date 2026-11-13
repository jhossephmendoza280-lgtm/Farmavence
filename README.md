# 💊 FarmaVence

Aplicación de escritorio en **Java (Swing)** para controlar los productos de una farmacia que están por vencer. Avisa de los medicamentos vencidos y de los que vencen en 1 o 2 meses, para poder rotarlos o retirarlos a tiempo.

![FarmaVence](FarmaVence.png)

## ¿Qué problema resuelve?

En una farmacia, un producto vencido es dinero perdido y un riesgo para el cliente. Llevar las fechas en un cuaderno hace fácil olvidarse de alguno. FarmaVence reemplaza el cuaderno: registras cada producto con su fecha de vencimiento y el programa te avisa cuáles necesitan atención.

## Funciones

- Registrar productos con **nombre, lote, stock y fecha de vencimiento**.
- **Alerta al abrir el programa** con los productos que requieren atención.
- Tabla ordenada del producto que vence primero al último, con colores:
  - 🔴 Rojo: ya vencido
  - 🟠 Naranja: vence en 30 días o menos (urgente)
  - 🟡 Amarillo: vence entre 31 y 60 días
  - ⚪ Blanco: sin problema
- Resumen con el total de vencidos, urgentes y por vencer.
- Filtro para ver todos los productos o solo las alertas.
- **Editar** y **eliminar** productos (con confirmación).
- **Validación de datos:** rechaza nombres vacíos y fechas inválidas (por ejemplo, 31/02).
- Los datos se **guardan automáticamente** en un archivo CSV.

## Tecnologías

- Java 21
- Java Swing (interfaz gráfica)
- `java.time` para el manejo de fechas
- Archivo CSV para guardar los datos

## Cómo ejecutarlo

Necesitas tener instalado un **JDK** (versión 17 o superior).

```bash
javac ControlVencimientosGUI.java
java ControlVencimientosGUI
```

## Dónde se guardan los datos

En la carpeta del usuario, en `ControlVencimientos/productos.csv`. Cada línea tiene este formato:

```
nombre,lote,stock,dd/MM/aaaa
```

## Crear una app para Windows (sin necesidad de tener Java instalado)

Con `jpackage`, que viene incluido en el JDK:

```bash
mkdir input
jar cfe input/FarmaVence.jar ControlVencimientosGUI ControlVencimientosGUI*.class
jpackage --type app-image --name FarmaVence --input input --main-jar FarmaVence.jar --main-class ControlVencimientosGUI --icon FarmaVence.ico
```

Esto genera una carpeta `FarmaVence` con `FarmaVence.exe`.

## Ideas para mejorar

- Búsqueda por nombre o lote.
- Exportar el reporte de alertas a un archivo.
- Sugerencia de descuento para productos próximos a vencer.
- Base de datos (SQLite o MySQL) en lugar de CSV.
- Versión web para usarla desde el celular.

## Autor

**Jhossep Henry Mendoza Santos** — estudiante de Ingeniería de Sistemas (UTP).
