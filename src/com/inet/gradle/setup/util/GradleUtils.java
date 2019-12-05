/*
 * Copyright 2019 i-net software
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.inet.gradle.setup.util;

import java.util.List;
import java.util.Objects;
import java.util.Set;

import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.tasks.TaskContainer;

public abstract class GradleUtils {

    /**
     * Checked if the given task will be executed because set in command line or depends on other executed task.
     * 
     * @param task the task to check
     * @param project the current project
     * @return true, if the task will be executed
     */
    public static boolean isTaskExecute( Task task, Project project ) {
        String name = task.getName();
        List<String> startTasks = project.getGradle().getStartParameter().getTaskNames();
        TaskContainer tasks = project.getTasks();
        for( String startTaskName : startTasks ) {
            if( Objects.equals( name, startTaskName ) ) {
                // direct call of the task in the command line
                return true;
            }
            try {
                Task startTask = tasks.getByName( startTaskName );
                if( isTaskExecute( task, name, startTask, tasks ) ) {
                    return true;
                }
            } catch( Throwable th ) {
                // can occur if there is a circle in the dependsOn
            }
        }

        return false;
    }

    /**
     * Checked if the given task will be executed because set in command line or depends on other executed task.
     * 
     * @param task the task to check
     * @param name the name to check
     * @param startTask a task that will be executed
     * @param tasks all tasks
     * @return true, if the task will be executed
     */
    private static boolean isTaskExecute( Task task, String name, Task startTask, TaskContainer tasks ) {
        if( startTask == null ) {
            return false;
        }
        Set<Object> dependsOn = startTask.getDependsOn(); // can contain a task name, a task or other objects
        for( Object depObject : dependsOn ) {
            if( Objects.equals( name, depObject ) ) {
                return true;
            }
            if( Objects.equals( task, depObject ) ) {
                return true;
            }
            Task depTask = null;
            if( depObject instanceof String ) {
                depTask = tasks.getByName( (String)depObject );
            } else if( depObject instanceof Task ) {
                depTask = (Task)depObject;
            }
            if( isTaskExecute( task, name, depTask, tasks ) ) {
                return true;
            }
        }
        return false;
    }
}
