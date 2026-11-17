package com.myesp32upgrade.microcontrol.ui.screens

import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.myesp32upgrade.microcontrol.model.Project
import com.myesp32upgrade.microcontrol.utils.StorageManager
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.ObjectInputStream
import java.io.ObjectOutputStream

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecentScreen(navController: NavController) {
    val context = LocalContext.current
    var projects by remember { mutableStateOf<List<Project>>(emptyList()) }
    var showDeleteDialog by remember { mutableStateOf<Project?>(null) }
    var showRenameDialog by remember { mutableStateOf<Project?>(null) }
    var newProjectName by remember { mutableStateOf("") }

    // Load projects when screen is created
    LaunchedEffect(Unit) {
        StorageManager.createPermanentProjectIfNotExists(context)
        loadProjects(context) { loadedProjects ->
            projects = loadedProjects
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Recent Projects") },
                actions = {
                    IconButton(onClick = {
                        loadProjects(context) { loadedProjects ->
                            projects = loadedProjects
                            Toast.makeText(context, "Projects refreshed", Toast.LENGTH_SHORT).show()
                        }
                    }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                    }
                }
            )
        }
    ) { paddingValues ->
        if (projects.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Text("No projects found")
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(projects) { project ->
                    ProjectCard(
                        project = project,
                        onProjectClick = {
                            navController.navigate(
                                com.myesp32upgrade.microcontrol.navigation.NavRoutes.Controller.createRoute(project.name)
                            )
                        },
                        onDeleteClick = { showDeleteDialog = project },
                        onRenameClick = { 
                            showRenameDialog = project
                            newProjectName = project.name
                        }
                    )
                }
            }
        }
    }

    // Delete Confirmation Dialog
    showDeleteDialog?.let { project ->
        AlertDialog(
            onDismissRequest = { showDeleteDialog = null },
            title = { Text("Delete Project") },
            text = { Text("Are you sure you want to delete '${project.name}'? This action cannot be undone.") },
            confirmButton = {
                Button(
                    onClick = {
                        deleteProject(context, project) {
                            projects = projects.filter { it.name != project.name }
                            showDeleteDialog = null
                            Toast.makeText(context, "Project deleted", Toast.LENGTH_SHORT).show()
                        }
                    }
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Rename Dialog
    showRenameDialog?.let { project ->
        AlertDialog(
            onDismissRequest = { showRenameDialog = null },
            title = { Text("Rename Project") },
            text = {
                OutlinedTextField(
                    value = newProjectName,
                    onValueChange = { newProjectName = it },
                    label = { Text("New Project Name") },
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newProjectName.isNotBlank()) {
                            renameProject(context, project, newProjectName) {
                                projects = projects.map { 
                                    if (it.name == project.name) it.copy(name = newProjectName) else it 
                                }
                                showRenameDialog = null
                                Toast.makeText(context, "Project renamed", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                ) {
                    Text("Rename")
                }
            },
            dismissButton = {
                TextButton(onClick = { showRenameDialog = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProjectCard(
    project: Project,
    onProjectClick: () -> Unit,
    onDeleteClick: () -> Unit,
    onRenameClick: () -> Unit
) {
    var showOptions by remember { mutableStateOf(false) }
    val context = LocalContext.current

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onProjectClick)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = project.name,
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = "${project.pinConfig.size} components",
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            Box {
                IconButton(onClick = { showOptions = true }) {
                    Icon(Icons.Default.MoreVert, contentDescription = "More options")
                }

                DropdownMenu(
                    expanded = showOptions,
                    onDismissRequest = { showOptions = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("Rename") },
                        onClick = {
                            showOptions = false
                            onRenameClick()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Delete") },
                        onClick = {
                            showOptions = false
                            onDeleteClick()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Save") },
                        onClick = {
                            showOptions = false
                            val success = StorageManager.saveProjectToSavedFolder(context, project.name)
                            Toast.makeText(context, if (success) "Project saved to SavedProjects" else "Failed to save project", Toast.LENGTH_SHORT).show()
                        }
                    )
                }
            }
        }
    }
}

private fun loadProjects(context: Context, onProjectsLoaded: (List<Project>) -> Unit) {
    val projectsFolder = StorageManager.getProjectsFolder(context) ?: return
    val projects = mutableListOf<Project>()

    projectsFolder.listFiles()?.forEach { projectFolder ->
        val projectFile = File(projectFolder, "project.dat")
        if (projectFile.exists()) {
            try {
                ObjectInputStream(FileInputStream(projectFile)).use { ois ->
                    val project = ois.readObject() as Project
                    projects.add(project)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    onProjectsLoaded(projects.sortedByDescending { it.createdAt })
}

private fun deleteProject(context: Context, project: Project, onSuccess: () -> Unit) {
    val projectsFolder = StorageManager.getProjectsFolder(context) ?: return
    val projectFolder = File(projectsFolder, project.name)
    
    if (projectFolder.exists()) {
        projectFolder.deleteRecursively()
        onSuccess()
    }
}

private fun renameProject(context: Context, project: Project, newName: String, onSuccess: () -> Unit) {
    val projectsFolder = StorageManager.getProjectsFolder(context) ?: return
    val oldFolder = File(projectsFolder, project.name)
    val newFolder = File(projectsFolder, newName)
    
    if (oldFolder.exists() && !newFolder.exists()) {
        if (oldFolder.renameTo(newFolder)) {
            val projectFile = File(newFolder, "project.dat")
            if (projectFile.exists()) {
                try {
                    ObjectInputStream(FileInputStream(projectFile)).use { ois ->
                        val updatedProject = ois.readObject() as Project
                        ObjectOutputStream(FileOutputStream(projectFile)).use { oos ->
                            oos.writeObject(updatedProject.copy(name = newName))
                        }
                    }
                    onSuccess()
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }
}

private fun loadProjectByName(context: Context, projectName: String): Project? {
    val projectsFolder = StorageManager.getProjectsFolder(context) ?: return null
    val projectFile = File(File(projectsFolder, projectName), "project.dat")
    if (projectFile.exists()) {
        ObjectInputStream(FileInputStream(projectFile)).use { ois ->
            return ois.readObject() as Project
        }
    }
    return null
}