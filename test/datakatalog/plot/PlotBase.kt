package datakatalog.plot

data class PlotBase (val layout : Layout, val data : List<Data>)



fun testPlot(yaxisText:String, data: List<Data>) = PlotBase(
    layout = Layout(
        xaxis = Axis(
            title = Title(
                text = "Dato",
                font = Font(16)
            )
        ),
        bargap = 0.1,
        title = Title(
            text = "Basic Histogram",
            font = Font(20)
        ),
        yaxis = Axis(
            title = Title(
                text = yaxisText,
                font = Font(16)
            )
        )
    ),
    data = data
)
