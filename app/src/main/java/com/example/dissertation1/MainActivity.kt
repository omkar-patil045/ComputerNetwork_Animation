package com.example.dissertation1

import android.os.Bundle
import android.widget.Toolbar
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateValue
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.updateTransition
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.tooling.preview.Preview
import com.example.dissertation1.ui.theme.Dissertation1Theme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.*
import androidx.compose.material3.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.lerp
import androidx.compose.ui.modifier.modifierLocalConsumer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.sqrt



class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            Dissertation1Theme {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    Navigation()
                }
            }
        }
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Navigation() {
    val navController = rememberNavController()

    NavHost(navController, startDestination = "mainScreen") {
        composable("mainScreen") { MainScreen(navController) }
        composable("circuitSwitching") { CircuitSwitchingPage(navController) }
        composable("packetSwitching") { PacketSwitchingPage(navController) }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(navController: NavHostController) {
    var nodes by rememberSaveable { mutableStateOf(mutableListOf<Node>()) }
    var draggingNodeIndex by remember { mutableStateOf(-1) }
    var touchPosition by remember { mutableStateOf(Offset(0f, 0f)) }
    var connections by rememberSaveable { mutableStateOf(emptyList<Connection>()) }
    var sourceNodeIndex by remember { mutableStateOf(-1) }

    val coroutineScope = rememberCoroutineScope()
    val packetAnimation = remember { Animatable(0f) }


    Surface(color = MaterialTheme.colorScheme.background) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {

            CenterAlignedTopAppBar(
                title = { Text(text = "Create Topology") },
                modifier = Modifier.background(Color.LightGray),
//                elevation = 4.dp
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    AddNodeButton { newNodePosition ->
                        val overlap =
                            nodes.any { isCircleOverlap(newNodePosition, it.position, 30f) }
                        if (!overlap) {
                            val newNode = Node(nodes.size, newNodePosition)
                            nodes = (nodes + newNode) as MutableList<Node>
                        }
                    }

                    DeleteLastNodeButton {
                        if (nodes.isNotEmpty()) {
                            connections =
                                connections.filter { it.fromNodeId != nodes.last().id && it.toNodeId != nodes.last().id }
                            nodes = nodes.dropLast(1).toMutableList()
                        }
                    }
                }

                ConnectNodesButton(nodes, connections) { sourceIndex, destinationIndex ->
                    if (sourceIndex != destinationIndex) {
                        connections = connections + Connection(nodes[sourceIndex].id, nodes[destinationIndex].id)
                    }
                }

                Spacer(modifier = Modifier.width(16.dp))
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Cyan)
                        .pointerInput(Unit) {
                            detectTransformGestures { _, pan, _, _ ->
                                touchPosition = touchPosition + pan
                            }
                        }
                ) {

                    val packetProgress = packetAnimation.value
                    val fromNode = nodes.getOrNull(0)
                    val toNode = nodes.getOrNull(nodes.size - 1)

                    if (fromNode != null && toNode != null) {
                        val packetX = lerp(fromNode.position.x, toNode.position.x, packetProgress)
                        val packetY = lerp(fromNode.position.y, toNode.position.y, packetProgress)
                        val packetWidth = 30f // Change the width of the rectangle
                        val packetHeight = 20f // Change the height of the rectangle
                        val packetPosition = Offset(packetX - packetWidth / 2, packetY - packetHeight / 2)
                        drawRect(
                            color = Color.Red,
                            size = Size(packetWidth, packetHeight),
                            topLeft = packetPosition
                        )
                    }

                    // Draw connections between nodes
                    for (connection in connections) {
                        val fromNode = nodes.find { it.id == connection.fromNodeId }
                        val toNode = nodes.find { it.id == connection.toNodeId }

                        if (fromNode != null && toNode != null) {
                            drawLine(
                                color = Color.Gray,
                                start = fromNode.position,
                                end = toNode.position,
                                strokeWidth = 2.dp.toPx()
                            )
                        }
                    }

                    // Draw nodes (circles)
                    for ((index, node) in nodes.withIndex()) {
                        drawCircle(
                            color = if (index == draggingNodeIndex) Color.Blue else Color.Black,
                            radius = 30f,
                            center = node.position
                        )
                        if (isInsideCircle(node.position, touchPosition, 30f)) {
                            draggingNodeIndex = index
                        }
                    }


                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Circuit Switching Button
            Button(
                onClick = {
                    // Navigate to Circuit Switching page
                    navController.navigate("circuitSwitching")
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            ) {
                Text("Circuit Switching")
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Packet Switching Button
            Button(
                onClick = {
                    // Navigate to Packet Switching page
                    navController.navigate("packetSwitching")
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            ) {
                Text("Packet Switching")
            }
        }

    }

    LaunchedEffect(key1 = true) {
        packetAnimation.animateTo(1f, animationSpec = tween(7000)) // 7 seconds
        coroutineScope.launch {
            delay(7000)
            packetAnimation.animateTo(0f, animationSpec = tween(1))
        }
    }
}


fun lerp(start: Float, stop: Float, fraction: Float): Float {
    return start + fraction * (stop - start)
}

enum class ConnectionState {
    IDLE, SOURCE_SELECTED
}

data class Node(
    val id: Int,
    val position: Offset
)

data class Connection(
    val fromNodeId: Int,
    val toNodeId: Int
)

@Composable
fun AddNodeButton(onAddNode: (Offset) -> Unit) {
    IconButton(
        onClick = {
            val newNodePosition = Offset((Math.random() * 500).toFloat(), (Math.random() * 500).toFloat())
            onAddNode(newNodePosition)
        }
    ) {
        Icon(
            imageVector = Icons.Default.Add,
            contentDescription = "Add Circle"
        )
    }
}

@Composable
fun DeleteLastNodeButton(onDeleteLastNode: () -> Unit) {
    IconButton(
        onClick = onDeleteLastNode,
        enabled = true // Set to false when no nodes to delete
    ) {
        Icon(
            imageVector = Icons.Default.Delete,
            contentDescription = "Delete Circle"
        )
    }
}

@Composable
fun ConnectNodesButton(nodes: List<Node>, connections: List<Connection>, onConnectNodes: (Int, Int) -> Unit) {
    var sourceIndex by remember { mutableStateOf(-1) }
    var destinationIndex by remember { mutableStateOf(-1) }

    Button(
        onClick = {
            if (sourceIndex != -1 && destinationIndex != -1) {
                onConnectNodes(sourceIndex, destinationIndex)
                sourceIndex = -1
                destinationIndex = -1
            } else {
                // Handle error or prompt user to select both source and destination
            }
        }
    ) {
        Text("Connect Nodes")
    }

    Column {
        Row {
            nodes.forEachIndexed { index, _ ->
                RadioButton(
                    selected = sourceIndex == index,
                    onClick = { sourceIndex = index },
                    colors = RadioButtonDefaults.colors(selectedColor = Color.Blue)
                )
            }
        }

        Row {
            nodes.forEachIndexed { index, _ ->
                RadioButton(
                    selected = destinationIndex == index,
                    onClick = { destinationIndex = index },
                    colors = RadioButtonDefaults.colors(selectedColor = Color.Red)
                )
            }
        }
    }
}

fun isInsideCircle(circle: Offset, point: Offset, radius: Float): Boolean {
    val dx = point.x - circle.x
    val dy = point.y - circle.y
    return dx * dx + dy * dy <= radius * radius
}

fun isCircleOverlap(position1: Offset, position2: Offset, radius: Float): Boolean {
    val dx = position1.x - position2.x
    val dy = position1.y - position2.y
    val distance = sqrt(dx * dx + dy * dy)
    val radiusSum = radius * 2 // Assuming the radius is the same for both circles
    return distance < radiusSum
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NumberInputField(label: String, value: String, onValueChange: (String) -> Unit) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Number),
        modifier = Modifier
            .width(IntrinsicSize.Min)
            .padding(4.dp)
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CircuitSwitchingPage(navController: NavHostController) {
    var messageLength by remember { mutableStateOf("") }
    var placementRate by remember { mutableStateOf("") }
    var setUpTime by remember { mutableStateOf("") }
    var maxPacketLength by remember { mutableStateOf("") }
    var headerLength by remember { mutableStateOf("") }
    var packetRoutingDelay by remember { mutableStateOf("") }
    var transmissionTime by remember { mutableStateOf("") }
    var throughput by remember { mutableStateOf("") }
    var totalTravelTime by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row() {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(8.dp)
            ) {
                NumberInputField("Message Length", messageLength) { value ->
                    messageLength = value
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text("bits", modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)

                Spacer(modifier = Modifier.height(8.dp))
                NumberInputField("Placement Rate", placementRate) { value ->
                    placementRate = value
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text("bits/sec", modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)

                Spacer(modifier = Modifier.height(8.dp))
                NumberInputField("Set Up Time", setUpTime) { value ->
                    setUpTime = value
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text("seconds", modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(8.dp)
            ) {
                NumberInputField("Max Packet Length", maxPacketLength) { value ->
                    maxPacketLength = value
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text("bits", modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)

                Spacer(modifier = Modifier.height(8.dp))
                NumberInputField("Header Length", headerLength) { value ->
                    headerLength = value
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text("bits", modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)

                Spacer(modifier = Modifier.height(8.dp))
                NumberInputField("Packet Routing Delay", packetRoutingDelay) { value ->
                    packetRoutingDelay = value
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text("seconds", modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
        OutlinedButton(
            onClick = {
                val transmissionRate = placementRate.toFloat()
                val transmissionTimeValue = setUpTime.toFloat() + (messageLength.toFloat() / transmissionRate)
                transmissionTime = transmissionTimeValue.toString()

            },
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
        ) {
            Text("Calculate Transmission Time")
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text("Transmission Time: $transmissionTime")

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedButton(
            onClick = {
                val messageLengthValue = messageLength.toFloat()
                val transmissionRateValue = placementRate.toFloat()
                val throughputValue = messageLengthValue / transmissionTime.toFloat()
                throughput = throughputValue.toString()
            },
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
        ) {
            Text("Calculate Throughput")
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text("Throughput: $throughput")

        // Button to calculate total travel time
        Spacer(modifier = Modifier.height(8.dp))

        OutlinedButton(
            onClick = {
                val transmissionTimeValue = transmissionTime.toFloat()
                val totalTravelTimeValue = transmissionTimeValue + setUpTime.toFloat()
                totalTravelTime = totalTravelTimeValue.toString()
            },
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
        ) {
            Text("Calculate Total Travel Time")
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text("Total Travel Time: $totalTravelTime")

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedButton(
            onClick = {
                navController.popBackStack()
                navController.navigate("mainScreen") {
                    launchSingleTop = true
                    restoreState = true
                }
            },
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
        ) {
            Text("Run Simulator")
        }
    }
}


// Function to calculate transmission time
fun calculateTransmissionTime(
    messageLength: Float,
    placementRate: Float,
    setUpTime: Float,
    maxPacketLength: Float,
    headerLength: Float,
    packetRoutingDelay: Float
): String {
    // Calculate transmission time based on the provided formula
    val transmissionTime = (messageLength / placementRate) +
            setUpTime + (maxPacketLength / placementRate) +
            (headerLength / placementRate) + packetRoutingDelay
    return transmissionTime.toString()
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PacketSwitchingPage(navController: NavHostController) {

    var messageLength by remember { mutableStateOf("") }
    var placementRate by remember { mutableStateOf("") }
    var setUpTime by remember { mutableStateOf("") }
    var maxPacketLength by remember { mutableStateOf("") }
    var headerLength by remember { mutableStateOf("") }
    var packetRoutingDelay by remember { mutableStateOf("") }
    var transmissionTime by remember { mutableStateOf("") }
    var throughput by remember { mutableStateOf("") }
    var totalTravelTime by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row() {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(8.dp)
            ) {
                NumberInputField("Message Length", messageLength) { value ->
                    messageLength = value
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text("bits", modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)

                Spacer(modifier = Modifier.height(8.dp))
                NumberInputField("Placement Rate", placementRate) { value ->
                    placementRate = value
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text("bits/sec", modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)

                Spacer(modifier = Modifier.height(8.dp))
                NumberInputField("Set Up Time", setUpTime) { value ->
                    setUpTime = value
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text("seconds", modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(8.dp)
            ) {
                NumberInputField("Max Packet Length", maxPacketLength) { value ->
                    maxPacketLength = value
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text("bits", modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)

                Spacer(modifier = Modifier.height(8.dp))
                NumberInputField("Header Length", headerLength) { value ->
                    headerLength = value
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text("bits", modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)

                Spacer(modifier = Modifier.height(8.dp))
                NumberInputField("Packet Routing Delay", packetRoutingDelay) { value ->
                    packetRoutingDelay = value
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text("seconds", modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedButton(
            onClick = {
                val placementRateValue = placementRate.toFloat()
                val packetSizeValue = maxPacketLength.toFloat()
                val packetRoutingDelayValue = packetRoutingDelay.toFloat()

                val transmissionTimeValue =
                    (messageLength.toFloat() / packetSizeValue) * (packetSizeValue / placementRateValue) + packetRoutingDelayValue

                transmissionTime = transmissionTimeValue.toString()
            },
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
        ) {
            Text("Calculate Transmission Time")
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text("Transmission Time: $transmissionTime")

        Spacer(modifier = Modifier.height(8.dp))

        // Button to calculate throughput
        OutlinedButton(
            onClick = {
                val placementRateValue = placementRate.toFloat()
                val packetSizeValue = maxPacketLength.toFloat()
                val packetRoutingDelayValue = packetRoutingDelay.toFloat()

                val throughputValue = (packetSizeValue / placementRateValue) * (1 / (1 + 2 * packetRoutingDelayValue))
                throughput = throughputValue.toString()
            },
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
        ) {
            Text("Calculate Throughput")
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text("Throughput: $throughput")

        // Button to calculate total travel time
        Spacer(modifier = Modifier.height(8.dp))

        OutlinedButton(
            onClick = {
                val transmissionTimeValue = transmissionTime.toFloat()
                val totalTravelTimeValue = transmissionTimeValue + packetRoutingDelay.toFloat()
                totalTravelTime = totalTravelTimeValue.toString()
            },
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
        ) {
            Text("Calculate Total Travel Time")
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text("Total Travel Time: $totalTravelTime")

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedButton(
            onClick = {
                navController.popBackStack()
                navController.navigate("mainScreen") {
                    launchSingleTop = true
                    restoreState = true
                }
            },
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
        ) {
            Text("Run Simulator")
        }
    }
}


//@Preview(showBackground = true,
//showSystemUi = true)
//@Composable
//fun GreetingPreview() {
//    Dissertation1Theme {
////        MainScreen(navController = )
//    }
//}