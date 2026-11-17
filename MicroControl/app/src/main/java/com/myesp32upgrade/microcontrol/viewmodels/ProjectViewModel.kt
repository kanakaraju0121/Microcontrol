package com.myesp32upgrade.microcontrol.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.myesp32upgrade.microcontrol.models.ProjectModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ProjectViewModel : ViewModel() {
    private val _projects = MutableStateFlow<List<ProjectModel>>(emptyList())
    val projects: StateFlow<List<ProjectModel>> = _projects.asStateFlow()

    fun addProject(project: ProjectModel) {
        viewModelScope.launch {
            val currentProjects = _projects.value.toMutableList()
            currentProjects.add(0, project)
            _projects.value = currentProjects
        }
    }

    fun deleteProject(project: ProjectModel) {
        viewModelScope.launch {
            val currentProjects = _projects.value.toMutableList()
            currentProjects.remove(project)
            _projects.value = currentProjects
        }
    }
} 