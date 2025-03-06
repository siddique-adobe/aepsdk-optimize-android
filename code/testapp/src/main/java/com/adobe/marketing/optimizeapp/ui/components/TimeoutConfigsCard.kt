package com.adobe.marketing.optimizeapp.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.Card
import androidx.compose.material.RadioButton
import androidx.compose.material.Text
import androidx.compose.material.TextField
import androidx.compose.material.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.adobe.marketing.optimizeapp.ui.model.TimeoutConfigsCardData

@Composable
fun TimeoutConfigsCard(
    data: TimeoutConfigsCardData,
    onOptionSelected: (Boolean) -> Unit = {},
    onTextChange: (String) -> Unit = {}
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        elevation = 4.dp,
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp)
        ) {
            Text(
                text = "GET & UPDATE propositions request timeout",
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                color = Color.Black,
                modifier = Modifier.padding(vertical = 12.dp)
            )
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Spacer(modifier = Modifier.width(8.dp))
                    RadioButton(
                        selected = !data.isCustomTimeoutOpted,
                        onClick = { onOptionSelected(false) }
                    )
                    Text(data.pref1Txt)
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Spacer(modifier = Modifier.width(8.dp))
                    RadioButton(
                        selected = data.isCustomTimeoutOpted,
                        onClick = { onOptionSelected(true) }
                    )
                    Text(data.pref2Txt)

                    if (data.isCustomTimeoutOpted) {
                        Spacer(modifier = Modifier.width(8.dp))
                        TextField(
                            textStyle = androidx.compose.ui.text.TextStyle(fontSize = 12.sp),
                            value = data.value,
                            onValueChange = { newValue ->
                                if (newValue.toDoubleOrNull() != null || newValue.isEmpty()) {
                                    onTextChange(newValue)
                                }
                            },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            shape = RoundedCornerShape(size = 10.dp),
                            colors = TextFieldDefaults.textFieldColors(
                                backgroundColor = Color.White,
                                focusedIndicatorColor = Color.Gray,
                                unfocusedIndicatorColor = Color.Gray
                            ),
                        )
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun PreviewTextFieldWithRadio() {
    TimeoutConfigsCard(
        TimeoutConfigsCardData(
            value = "30",
            isCustomTimeoutOpted = true,
            pref1Txt = "Default timeout | Config timeout",
            pref2Txt = "Custom timeout"
        )
    )
}
