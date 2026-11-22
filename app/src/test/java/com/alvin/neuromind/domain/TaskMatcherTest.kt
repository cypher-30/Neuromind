package com.alvin.neuromind.domain

import com.alvin.neuromind.data.Task
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TaskMatcherTest {
    private val tasks = listOf(
        Task(id = 1, title = "Crypto assignment 2"),
        Task(id = 2, title = "Mobile app notebook"),
        Task(id = 3, title = "Read AI chapter 5"),
        Task(id = 4, title = "Read Graphics notes"),
        Task(id = 5, title = "Old thing", isCompleted = true)
    )

    @Test fun `substring is exact`() =
        assertEquals(1, (TaskMatcher.match("crypto assignment", tasks) as TaskMatch.Exact).task.id)

    @Test fun `prefixes and word order still match`() =
        assertEquals(2, (TaskMatcher.match("notebook mobile", tasks) as TaskMatch.Exact).task.id)

    @Test fun `a typo is likely, so Ask confirms`() =
        assertEquals(1, (TaskMatcher.match("crypto asignment", tasks) as TaskMatch.Likely).task.id)

    @Test fun `shared word gives several`() =
        assertTrue(TaskMatcher.match("read", tasks) is TaskMatch.Several)

    @Test fun `unrelated words and completed tasks give none`() {
        assertEquals(TaskMatch.None, TaskMatcher.match("laundry", tasks))
        assertEquals(TaskMatch.None, TaskMatcher.match("old thing", tasks))
    }
}
