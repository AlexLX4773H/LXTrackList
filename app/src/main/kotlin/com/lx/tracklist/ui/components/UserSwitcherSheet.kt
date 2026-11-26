package com.lx.tracklist.ui.components

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.lx.tracklist.data.local.StorageHelper
import com.lx.tracklist.data.model.UserProfile
import com.lx.tracklist.ui.settings.ColorWheelPicker
import com.lx.tracklist.ui.theme.LXBackgroundBlack
import com.lx.tracklist.ui.theme.LXBorder
import com.lx.tracklist.ui.theme.LXErrorRed
import com.lx.tracklist.ui.theme.LXPrimaryGreen
import com.lx.tracklist.ui.theme.LXSurfaceBlack
import com.lx.tracklist.ui.theme.LXSurfaceElevated
import com.lx.tracklist.ui.theme.LXTextPrimary
import com.lx.tracklist.ui.theme.LXTextSecondary
import com.lx.tracklist.ui.theme.parseHexColor

private sealed interface PendingPasskeyAction {
    val user: UserProfile
    data class Select(override val user: UserProfile) : PendingPasskeyAction
    data class Edit(override val user: UserProfile) : PendingPasskeyAction
    data class Delete(override val user: UserProfile) : PendingPasskeyAction
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserSwitcherSheet(
    activeUser: UserProfile,
    allUsers: List<UserProfile>,
    requirePinForActive: Boolean = false,
    onSelectUser: (UserProfile, Boolean) -> Unit,
    onCreateUser: (String, String, String?, String?) -> Unit,
    onUpdateUser: (UserProfile) -> Unit = {},
    onDeleteUser: (UserProfile) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val sheetState = rememberModalBottomSheetState()
    val avatarColors = listOf("#A7EF60", "#4DA8DA", "#BB86FC", "#FF7597", "#FFD700", "#00E5FF", "#FF8A65")

    var isAddingUser by remember { mutableStateOf(false) }
    var newUserName by remember { mutableStateOf("") }
    var selectedColor by remember { mutableStateOf(avatarColors.first()) }
    var customAvatarColor by remember { mutableStateOf<String?>(null) }
    var showCustomColorPicker by remember { mutableStateOf(false) }
    var newAvatarUri by remember { mutableStateOf<String?>(null) }
    var isProtectWithPasskey by remember { mutableStateOf(false) }
    var newPasskey by remember { mutableStateOf("") }

    var pendingPasskeyAction by remember { mutableStateOf<PendingPasskeyAction?>(null) }

    var userToDelete by remember { mutableStateOf<UserProfile?>(null) }
    var userToEdit by remember { mutableStateOf<UserProfile?>(null) }
    var editUserName by remember { mutableStateOf("") }
    var editAvatarColor by remember { mutableStateOf(avatarColors.first()) }
    var editCustomAvatarColor by remember { mutableStateOf<String?>(null) }
    var showEditCustomColorPicker by remember { mutableStateOf(false) }
    var editAvatarUri by remember { mutableStateOf<String?>(null) }

    fun openEditDialog(user: UserProfile) {
        userToEdit = user
        editUserName = user.name
        editAvatarColor = user.avatarColorHex
        editCustomAvatarColor = if (!avatarColors.any { it.equals(user.avatarColorHex, ignoreCase = true) }) user.avatarColorHex else null
        editAvatarUri = user.avatarImageUri
    }

    var isPickingForEdit by remember { mutableStateOf(false) }
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            val savedPath = StorageHelper.saveAvatarImage(context, it)
            if (isPickingForEdit) {
                editAvatarUri = savedPath
            } else {
                newAvatarUri = savedPath
            }
        }
    }

    // Delete Profile Confirmation Dialog
    if (userToDelete != null) {
        AlertDialog(
            onDismissRequest = { userToDelete = null },
            title = { Text("Delete Profile?", color = LXTextPrimary, fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    "Are you sure you want to delete profile \"${userToDelete?.name}\"? All their tracked media will also be removed.",
                    color = LXTextSecondary
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        userToDelete?.let { onDeleteUser(it) }
                        userToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = LXErrorRed)
                ) {
                    Text("Delete", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { userToDelete = null }) {
                    Text("Cancel", color = LXTextSecondary)
                }
            },
            containerColor = LXSurfaceElevated
        )
    }

    // Edit Profile Dialog
    if (userToEdit != null) {
        AlertDialog(
            onDismissRequest = { userToEdit = null },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Edit, contentDescription = null, tint = LXPrimaryGreen, modifier = Modifier.size(22.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Edit Profile", color = LXTextPrimary, fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Profile Photo section
                    Text("Profile Photo", style = MaterialTheme.typography.labelMedium, color = LXTextSecondary)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        val color = parseHexColor(editAvatarColor)
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(CircleShape)
                                .background(color)
                                .border(2.dp, color, CircleShape)
                                .clickable {
                                    isPickingForEdit = true
                                    photoPickerLauncher.launch("image/*")
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            if (!editAvatarUri.isNullOrBlank()) {
                                AsyncImage(
                                    model = editAvatarUri,
                                    contentDescription = "Avatar",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .clip(CircleShape)
                                        .border(2.dp, color, CircleShape)
                                )
                            } else {
                                Text(
                                    text = editUserName.take(1).uppercase().ifBlank { "?" },
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 24.sp,
                                    color = if (isColorDark(color)) Color.White else Color.Black
                                )
                            }
                        }

                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            OutlinedButton(
                                onClick = {
                                    isPickingForEdit = true
                                    photoPickerLauncher.launch("image/*")
                                },
                                shape = RoundedCornerShape(8.dp),
                                border = BorderStroke(1.dp, LXBorder),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = LXTextPrimary)
                            ) {
                                Icon(Icons.Default.Image, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(if (!editAvatarUri.isNullOrBlank()) "Change Photo" else "Choose Photo", fontSize = 12.sp)
                            }

                            if (!editAvatarUri.isNullOrBlank()) {
                                TextButton(onClick = { editAvatarUri = null }) {
                                    Text("Remove Photo", color = LXErrorRed, fontSize = 11.sp)
                                }
                            }
                        }
                    }

                    Text("Profile Name", style = MaterialTheme.typography.labelMedium, color = LXTextSecondary)
                    OutlinedTextField(
                        value = editUserName,
                        onValueChange = { editUserName = it },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = LXPrimaryGreen,
                            unfocusedBorderColor = LXBorder,
                            focusedTextColor = LXTextPrimary,
                            unfocusedTextColor = LXTextPrimary,
                            cursorColor = LXPrimaryGreen
                        )
                    )

                    Text("Avatar Color", style = MaterialTheme.typography.labelMedium, color = LXTextSecondary)
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        items(avatarColors) { hex ->
                            val color = parseHexColor(hex)
                            val isChosen = editAvatarColor.equals(hex, ignoreCase = true)
                            Box(
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(CircleShape)
                                    .background(color)
                                    .border(
                                        width = if (isChosen) 3.dp else 1.dp,
                                        color = if (isChosen) Color.White else Color.Transparent,
                                        shape = CircleShape
                                    )
                                    .clickable { editAvatarColor = hex },
                                contentAlignment = Alignment.Center
                            ) {
                                if (isChosen) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = null,
                                        tint = if (isColorDark(color)) Color.White else Color.Black,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }

                        val currentEditCustomColor = editCustomAvatarColor
                        if (currentEditCustomColor != null && !avatarColors.any { it.equals(currentEditCustomColor, ignoreCase = true) }) {
                            item {
                                val color = parseHexColor(currentEditCustomColor)
                                val isChosen = editAvatarColor.equals(currentEditCustomColor, ignoreCase = true)
                                Box(
                                    modifier = Modifier
                                        .size(34.dp)
                                        .clip(CircleShape)
                                        .background(color)
                                        .border(
                                            width = if (isChosen) 3.dp else 1.dp,
                                            color = if (isChosen) Color.White else Color.Transparent,
                                            shape = CircleShape
                                        )
                                        .clickable {
                                            if (isChosen) {
                                                showEditCustomColorPicker = true
                                            } else {
                                                editAvatarColor = currentEditCustomColor
                                            }
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (isChosen) {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = null,
                                            tint = if (isColorDark(color)) Color.White else Color.Black,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }
                        }

                        item {
                            Box(
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(CircleShape)
                                    .background(LXBackgroundBlack)
                                    .border(
                                        width = 1.dp,
                                        color = LXBorder,
                                        shape = CircleShape
                                    )
                                    .clickable { showEditCustomColorPicker = true },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = "Set Custom Avatar Color",
                                    tint = LXPrimaryGreen,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val current = userToEdit ?: return@Button
                        val trimmedName = editUserName.trim()
                        if (trimmedName.isNotBlank()) {
                            onUpdateUser(
                                current.copy(
                                    name = trimmedName,
                                    avatarColorHex = editAvatarColor,
                                    avatarImageUri = editAvatarUri
                                )
                            )
                            userToEdit = null
                            editCustomAvatarColor = null
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = LXPrimaryGreen, contentColor = LXBackgroundBlack),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Save Changes", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    userToEdit = null
                    editCustomAvatarColor = null
                }) {
                    Text("Cancel", color = LXTextSecondary)
                }
            },
            containerColor = LXSurfaceElevated
        )
    }

    // Custom Avatar Color Dialog for Create Profile
    if (showCustomColorPicker) {
        CustomAvatarColorDialog(
            initialColorHex = selectedColor,
            onDismiss = { showCustomColorPicker = false },
            onColorSelected = { chosenColor ->
                customAvatarColor = chosenColor
                selectedColor = chosenColor
            }
        )
    }

    // Custom Avatar Color Dialog for Edit Profile
    if (showEditCustomColorPicker) {
        CustomAvatarColorDialog(
            initialColorHex = editAvatarColor,
            onDismiss = { showEditCustomColorPicker = false },
            onColorSelected = { chosenColor ->
                editCustomAvatarColor = chosenColor
                editAvatarColor = chosenColor
            }
        )
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = LXSurfaceBlack,
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(vertical = 10.dp)
                    .width(36.dp)
                    .height(4.dp)
                    .clip(CircleShape)
                    .background(LXBorder)
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = null,
                        tint = LXPrimaryGreen,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "User Profiles",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = LXTextPrimary
                    )
                }

                if (!isAddingUser) {
                    TextButton(onClick = { isAddingUser = true }) {
                        Icon(Icons.Default.Add, contentDescription = null, tint = LXPrimaryGreen, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Add Profile", color = LXPrimaryGreen, fontWeight = FontWeight.Bold)
                    }
                }
            }

            HorizontalDivider(color = LXBorder, thickness = 1.dp, modifier = Modifier.padding(vertical = 10.dp))

            if (isAddingUser) {
                // Add Profile Form
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(LXSurfaceElevated)
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text("Profile Photo", style = MaterialTheme.typography.labelMedium, color = LXTextPrimary)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        val color = parseHexColor(selectedColor)
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .clip(CircleShape)
                                .background(color)
                                .border(2.dp, color, CircleShape)
                                .clickable {
                                    isPickingForEdit = false
                                    photoPickerLauncher.launch("image/*")
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            if (!newAvatarUri.isNullOrBlank()) {
                                AsyncImage(
                                    model = newAvatarUri,
                                    contentDescription = "Avatar Preview",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .clip(CircleShape)
                                        .border(2.dp, color, CircleShape)
                                )
                            } else {
                                Text(
                                    text = newUserName.take(1).uppercase().ifBlank { "?" },
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 22.sp,
                                    color = if (isColorDark(color)) Color.White else Color.Black
                                )
                            }
                        }

                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            OutlinedButton(
                                onClick = {
                                    isPickingForEdit = false
                                    photoPickerLauncher.launch("image/*")
                                },
                                shape = RoundedCornerShape(8.dp),
                                border = BorderStroke(1.dp, LXBorder),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = LXTextPrimary)
                            ) {
                                Icon(Icons.Default.Image, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(if (!newAvatarUri.isNullOrBlank()) "Change Photo" else "Choose Photo", fontSize = 12.sp)
                            }

                            if (!newAvatarUri.isNullOrBlank()) {
                                TextButton(onClick = { newAvatarUri = null }) {
                                    Text("Remove Photo", color = LXErrorRed, fontSize = 11.sp)
                                }
                            }
                        }
                    }

                    Text("New Profile Name", style = MaterialTheme.typography.labelMedium, color = LXTextPrimary)
                    OutlinedTextField(
                        value = newUserName,
                        onValueChange = { newUserName = it },
                        placeholder = { Text("e.g. Alex, Guest, Anime Only...", color = LXTextSecondary) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = LXPrimaryGreen,
                            unfocusedBorderColor = LXBorder,
                            focusedTextColor = LXTextPrimary,
                            unfocusedTextColor = LXTextPrimary,
                            cursorColor = LXPrimaryGreen
                        )
                    )

                    Text("Avatar Color", style = MaterialTheme.typography.labelMedium, color = LXTextPrimary)
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        items(avatarColors) { hex ->
                            val color = parseHexColor(hex)
                            val isChosen = selectedColor.equals(hex, ignoreCase = true)
                            Box(
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(CircleShape)
                                    .background(color)
                                    .border(
                                        width = if (isChosen) 3.dp else 1.dp,
                                        color = if (isChosen) Color.White else Color.Transparent,
                                        shape = CircleShape
                                    )
                                    .clickable { selectedColor = hex },
                                contentAlignment = Alignment.Center
                            ) {
                                if (isChosen) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = null,
                                        tint = if (isColorDark(color)) Color.White else Color.Black,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }

                        val currentCustom = customAvatarColor
                        if (currentCustom != null && !avatarColors.any { it.equals(currentCustom, ignoreCase = true) }) {
                            item {
                                val color = parseHexColor(currentCustom)
                                val isChosen = selectedColor.equals(currentCustom, ignoreCase = true)
                                Box(
                                    modifier = Modifier
                                        .size(34.dp)
                                        .clip(CircleShape)
                                        .background(color)
                                        .border(
                                            width = if (isChosen) 3.dp else 1.dp,
                                            color = if (isChosen) Color.White else Color.Transparent,
                                            shape = CircleShape
                                        )
                                        .clickable {
                                            if (isChosen) {
                                                showCustomColorPicker = true
                                            } else {
                                                selectedColor = currentCustom
                                            }
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (isChosen) {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = null,
                                            tint = if (isColorDark(color)) Color.White else Color.Black,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }
                        }

                        item {
                            Box(
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(CircleShape)
                                    .background(LXBackgroundBlack)
                                    .border(
                                        width = 1.dp,
                                        color = LXBorder,
                                        shape = CircleShape
                                    )
                                    .clickable { showCustomColorPicker = true },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = "Set Custom Avatar Color",
                                    tint = LXPrimaryGreen,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }

                    // Passkey toggle & input
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Protect with 4-Digit Passkey", style = MaterialTheme.typography.labelMedium, color = LXTextPrimary)
                            Text("Requires passkey when switching to this profile", style = MaterialTheme.typography.bodySmall, color = LXTextSecondary, fontSize = 11.sp)
                        }
                        androidx.compose.material3.Switch(
                            checked = isProtectWithPasskey,
                            onCheckedChange = {
                                isProtectWithPasskey = it
                                if (!it) newPasskey = ""
                            },
                            colors = androidx.compose.material3.SwitchDefaults.colors(
                                checkedThumbColor = LXBackgroundBlack,
                                checkedTrackColor = LXPrimaryGreen,
                                uncheckedThumbColor = LXTextSecondary,
                                uncheckedTrackColor = LXSurfaceElevated
                            )
                        )
                    }

                    if (isProtectWithPasskey) {
                        OutlinedTextField(
                            value = newPasskey,
                            onValueChange = { input ->
                                val digits = input.filter { it.isDigit() }.take(4)
                                newPasskey = digits
                            },
                            placeholder = { Text("4-digit PIN (e.g. 1234)", color = LXTextSecondary) },
                            singleLine = true,
                            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                                keyboardType = androidx.compose.ui.text.input.KeyboardType.NumberPassword
                            ),
                            visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation(),
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = LXPrimaryGreen,
                                unfocusedBorderColor = LXBorder,
                                focusedTextColor = LXTextPrimary,
                                unfocusedTextColor = LXTextPrimary,
                                cursorColor = LXPrimaryGreen
                            ),
                            supportingText = {
                                if (newPasskey.isNotEmpty() && newPasskey.length < 4) {
                                    Text("${newPasskey.length}/4 digits entered", color = LXErrorRed, fontSize = 11.sp)
                                } else if (newPasskey.length == 4) {
                                    Text("4-digit passkey set", color = LXPrimaryGreen, fontSize = 11.sp)
                                }
                            }
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(onClick = {
                            isAddingUser = false
                            newUserName = ""
                            selectedColor = avatarColors.first()
                            customAvatarColor = null
                            newAvatarUri = null
                            isProtectWithPasskey = false
                            newPasskey = ""
                        }) {
                            Text("Cancel", color = LXTextSecondary)
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        Button(
                            onClick = {
                                if (newUserName.isNotBlank() && (!isProtectWithPasskey || newPasskey.length == 4)) {
                                    val finalPass = if (isProtectWithPasskey && newPasskey.length == 4) newPasskey else null
                                    onCreateUser(newUserName, selectedColor, newAvatarUri, finalPass)
                                    isAddingUser = false
                                    newUserName = ""
                                    selectedColor = avatarColors.first()
                                    customAvatarColor = null
                                    newAvatarUri = null
                                    isProtectWithPasskey = false
                                    newPasskey = ""
                                    onDismiss()
                                }
                            },
                            enabled = newUserName.isNotBlank() && (!isProtectWithPasskey || newPasskey.length == 4),
                            colors = ButtonDefaults.buttonColors(containerColor = LXPrimaryGreen, contentColor = LXBackgroundBlack),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Create Profile", fontWeight = FontWeight.Bold)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
            }

            // Existing Profiles List
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                allUsers.forEach { user ->
                    val isActive = user.id == activeUser.id
                    val avatarColor = parseHexColor(user.avatarColorHex)

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (isActive) LXPrimaryGreen.copy(alpha = 0.12f) else LXSurfaceElevated)
                            .border(
                                width = 1.dp,
                                color = if (isActive) LXPrimaryGreen else LXBorder,
                                shape = RoundedCornerShape(10.dp)
                            )
                            .clickable {
                                if (shouldRequirePinForProfileSelection(user.passKey, isActive, requirePinForActive)) {
                                    pendingPasskeyAction = PendingPasskeyAction.Select(user)
                                } else {
                                    onSelectUser(user, false)
                                    onDismiss()
                                }
                            }
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(avatarColor)
                                    .border(1.5.dp, avatarColor, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                if (!user.avatarImageUri.isNullOrBlank()) {
                                    AsyncImage(
                                        model = user.avatarImageUri,
                                        contentDescription = user.name,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .clip(CircleShape)
                                            .border(1.5.dp, avatarColor, CircleShape)
                                    )
                                } else {
                                    Text(
                                        text = user.name.take(1).uppercase(),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 16.sp,
                                        color = if (isColorDark(avatarColor)) Color.White else Color.Black
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = user.name,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isActive) LXPrimaryGreen else LXTextPrimary
                                    )
                                    if (!user.passKey.isNullOrBlank()) {
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Icon(
                                            imageVector = Icons.Default.Lock,
                                            contentDescription = "Passkey Protected",
                                            tint = LXPrimaryGreen,
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }
                                }
                                if (isActive) {
                                    Text("Active Profile", fontSize = 11.sp, color = LXPrimaryGreen)
                                } else if (!user.passKey.isNullOrBlank()) {
                                    Text("Passkey Protected", fontSize = 11.sp, color = LXTextSecondary)
                                }
                            }
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = {
                                if (!user.passKey.isNullOrBlank()) {
                                    pendingPasskeyAction = PendingPasskeyAction.Edit(user)
                                } else {
                                    openEditDialog(user)
                                }
                            }) {
                                Icon(
                                    imageVector = Icons.Default.Edit,
                                    contentDescription = "Edit Profile",
                                    tint = LXTextSecondary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }

                            if (isActive) {
                                Icon(Icons.Default.Check, contentDescription = "Active", tint = LXPrimaryGreen, modifier = Modifier.size(20.dp))
                            } else if (allUsers.size > 1) {
                                IconButton(onClick = {
                                    if (!user.passKey.isNullOrBlank()) {
                                        pendingPasskeyAction = PendingPasskeyAction.Delete(user)
                                    } else {
                                        userToDelete = user
                                    }
                                }) {
                                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = LXTextSecondary, modifier = Modifier.size(18.dp))
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }

    pendingPasskeyAction?.let { action ->
        val targetUser = action.user
        val pass = targetUser.passKey ?: ""
        val (dialogTitle, dialogSubtitle) = when (action) {
            is PendingPasskeyAction.Select -> "Unlock Profile" to "Enter 4-digit passkey to switch to \"${targetUser.name}\""
            is PendingPasskeyAction.Edit -> "Verify Passkey" to "Enter 4-digit passkey to edit \"${targetUser.name}\""
            is PendingPasskeyAction.Delete -> "Verify Passkey" to "Enter 4-digit passkey to delete \"${targetUser.name}\""
        }
        PasskeyVerificationDialog(
            profileName = targetUser.name,
            expectedPasskey = pass,
            profileId = targetUser.id,
            title = dialogTitle,
            subtitle = dialogSubtitle,
            onSuccess = {
                pendingPasskeyAction = null
                when (action) {
                    is PendingPasskeyAction.Select -> {
                        onSelectUser(targetUser, true)
                        onDismiss()
                    }
                    is PendingPasskeyAction.Edit -> openEditDialog(targetUser)
                    is PendingPasskeyAction.Delete -> userToDelete = targetUser
                }
            },
            onDismiss = {
                pendingPasskeyAction = null
            }
        )
    }
}

private fun isColorDark(color: Color): Boolean {
    val luminance = 0.299f * color.red + 0.587f * color.green + 0.114f * color.blue
    return luminance < 0.5f
}

@Composable
private fun CustomAvatarColorDialog(
    initialColorHex: String,
    onDismiss: () -> Unit,
    onColorSelected: (String) -> Unit
) {
    var tempColorHex by remember(initialColorHex) {
        val clean = initialColorHex.trim()
        val hex = if (clean.startsWith("#")) clean else "#$clean"
        mutableStateOf(hex)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
        modifier = Modifier.fillMaxWidth(0.92f),
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Palette,
                    contentDescription = null,
                    tint = LXPrimaryGreen,
                    modifier = Modifier.size(22.dp)
                )
                Text(
                    text = "Custom Avatar Color",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = LXTextPrimary
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                ColorWheelPicker(
                    selectedHex = tempColorHex,
                    onColorChanged = { tempColorHex = it }
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onColorSelected(tempColorHex)
                    onDismiss()
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = LXPrimaryGreen,
                    contentColor = LXBackgroundBlack
                ),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("Select Color", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = LXTextSecondary)
            }
        },
        containerColor = LXSurfaceElevated,
        shape = RoundedCornerShape(16.dp)
    )
}
