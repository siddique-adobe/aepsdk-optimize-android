package com.adobe.marketing.optimizeapp.ui.compose

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.adobe.marketing.optimizeapp.R
import com.adobe.marketing.optimizeapp.models.LogEntry
import com.adobe.marketing.optimizeapp.viewmodels.MainViewModel
import kotlinx.coroutines.launch

@Composable
fun LogBox(
    modifier: Modifier = Modifier,
    viewModel: MainViewModel
) {
    var expanded by remember { mutableStateOf(false) }
    val logs = viewModel.logManager.logs
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    Box(
        modifier = modifier
            .background(color = Color(0xFF3949AB))
            .padding(8.dp)
    ) {
        Column {
            Text(
                text = "Logs",
                fontSize = 16.sp,
                color = Color.White,
                modifier = Modifier.padding(vertical = 8.dp)
            )
            if (expanded && logs.isNotEmpty()) {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(logs.size) { index ->
                        LogView(logs[index])
                    }
                }

                LaunchedEffect(logs.size) {
                    if (logs.isNotEmpty()) {
                        coroutineScope.launch {
                            listState.animateScrollToItem(logs.size - 1)
                        }
                    }
                }
            } else if (logs.isEmpty()) {
                LogView(LogEntry("No logs available", ""))
            } else {
                LogView(logs.last())
            }
        }

        IconButton(
            onClick = { expanded = !expanded },
            modifier = Modifier
                .align(Alignment.BottomEnd)
        ) {
            Icon(
                painter = if (expanded)
                    painterResource(R.drawable.ic_arrow_down)
                else
                    painterResource(R.drawable.ic_arrow_up),
                tint = Color.White,
                contentDescription = if (expanded) "Collapse" else "Expand"
            )
        }
    }
}

@Composable
private fun LogView(log: LogEntry) {
    Text(
        text = "${log.timestamp}: ${log.text}",
        color = Color.White,
        modifier = Modifier.padding(vertical = 2.dp),
        maxLines = 2,
        overflow = TextOverflow.Ellipsis,
        style = MaterialTheme.typography.body2
    )
}