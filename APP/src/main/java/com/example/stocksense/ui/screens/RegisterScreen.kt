package com.example.stocksense.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.stocksense.data.model.Store
import com.example.stocksense.data.model.User
import com.example.stocksense.data.model.UserRole
import com.example.stocksense.ui.StockViewModel
import com.example.stocksense.ui.navigation.Screen
import com.example.stocksense.ui.theme.Navy
import com.example.stocksense.ui.theme.Silver

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegisterScreen(
    viewModel: StockViewModel,
    navController: NavController
) {
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var selectedRole by remember { mutableStateOf(UserRole.STORE_MANAGER) }
    var selectedStoreId by remember { mutableStateOf("") }
    var isCreatingNewStore by remember { mutableStateOf(false) }
    
    // New Store Fields
    var businessName by remember { mutableStateOf("") }
    var storeName by remember { mutableStateOf("") }
    var city by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }
    var warehouseCapacity by remember { mutableStateOf("5000") }

    var expandedRole by remember { mutableStateOf(false) }
    var expandedStore by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val stores by viewModel.stores.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Create Account", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Silver)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Navy, titleContentColor = Silver)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Account Details",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.align(Alignment.Start)
            )

            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Full Name *") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text("Email Address *") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
            )

            ExposedDropdownMenuBox(
                expanded = expandedRole,
                onExpandedChange = { expandedRole = !expandedRole },
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedTextField(
                    value = if (selectedRole == UserRole.ADMIN) "Inventory Manager (Admin)" else "Store Manager",
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Select Role") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedRole) },
                    modifier = Modifier.menuAnchor().fillMaxWidth()
                )
                ExposedDropdownMenu(
                    expanded = expandedRole,
                    onDismissRequest = { expandedRole = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("Inventory Manager (Admin)") },
                        onClick = {
                            selectedRole = UserRole.ADMIN
                            expandedRole = false
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Store Manager") },
                        onClick = {
                            selectedRole = UserRole.STORE_MANAGER
                            expandedRole = false
                        }
                    )
                }
            }

            if (selectedRole == UserRole.STORE_MANAGER) {
                HorizontalDivider()
                Text(
                    text = "Store Assignment",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.align(Alignment.Start)
                )

                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    RadioButton(selected = !isCreatingNewStore, onClick = { isCreatingNewStore = false })
                    Text("Select Existing Store", modifier = Modifier.padding(start = 8.dp))
                }

                if (!isCreatingNewStore) {
                    ExposedDropdownMenuBox(
                        expanded = expandedStore,
                        onExpandedChange = { expandedStore = !expandedStore },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OutlinedTextField(
                            value = stores.find { it.id == selectedStoreId }?.name ?: "Choose Store",
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Assigned Store *") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedStore) },
                            modifier = Modifier.menuAnchor().fillMaxWidth()
                        )
                        ExposedDropdownMenu(
                            expanded = expandedStore,
                            onDismissRequest = { expandedStore = false }
                        ) {
                            stores.forEach { store ->
                                DropdownMenuItem(
                                    text = { Text("${store.name} (${store.city})") },
                                    onClick = {
                                        selectedStoreId = store.id
                                        expandedStore = false
                                    }
                                )
                            }
                        }
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    RadioButton(selected = isCreatingNewStore, onClick = { isCreatingNewStore = true })
                    Text("Register New Store", modifier = Modifier.padding(start = 8.dp))
                }

                if (isCreatingNewStore) {
                    OutlinedTextField(value = businessName, onValueChange = { businessName = it }, label = { Text("Business Name *") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = storeName, onValueChange = { storeName = it }, label = { Text("Store Name *") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = city, onValueChange = { city = it }, label = { Text("City *") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = address, onValueChange = { address = it }, label = { Text("Address") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = warehouseCapacity, onValueChange = { warehouseCapacity = it }, label = { Text("Warehouse Capacity") }, modifier = Modifier.fillMaxWidth(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                }
            }

            HorizontalDivider()
            Text(
                text = "Security",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.align(Alignment.Start)
            )

            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("Password *") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                visualTransformation = PasswordVisualTransformation()
            )

            OutlinedTextField(
                value = confirmPassword,
                onValueChange = { confirmPassword = it },
                label = { Text("Confirm Password *") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                visualTransformation = PasswordVisualTransformation()
            )

            if (errorMessage != null) {
                Text(errorMessage!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            }

            Button(
                onClick = {
                    if (name.isBlank() || email.isBlank() || password.isBlank()) {
                        errorMessage = "All mandatory fields (*) are required."
                        return@Button
                    }
                    if (password != confirmPassword) {
                        errorMessage = "Passwords do not match."
                        return@Button
                    }
                    
                    if (selectedRole == UserRole.STORE_MANAGER) {
                        if (isCreatingNewStore) {
                            if (businessName.isBlank() || storeName.isBlank() || city.isBlank()) {
                                errorMessage = "Store details are required."
                                return@Button
                            }
                            val newStoreId = "S_NEW_${System.currentTimeMillis()}"
                            viewModel.addStore(Store(
                                id = newStoreId,
                                name = storeName,
                                businessName = businessName,
                                city = city,
                                address = address,
                                warehouseCapacity = warehouseCapacity.toIntOrNull() ?: 5000
                            ))
                            selectedStoreId = newStoreId
                        } else if (selectedStoreId.isBlank()) {
                            errorMessage = "Please assign a store."
                            return@Button
                        }
                    }

                    // For prototype, navigate to login after successful register
                    navController.navigate(Screen.Login.route) {
                        popUpTo(Screen.Welcome.route) { inclusive = false }
                    }
                },
                modifier = Modifier.fillMaxWidth().height(50.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Navy)
            ) {
                Text("CREATE ACCOUNT", fontWeight = FontWeight.Bold)
            }
            
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
