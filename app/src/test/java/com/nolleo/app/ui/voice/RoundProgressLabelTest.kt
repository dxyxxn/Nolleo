package com.nolleo.app.ui.voice

import com.nolleo.app.domain.engine.SessionState
import com.nolleo.app.domain.model.GameKind
import com.nolleo.app.domain.model.SessionPlan
import org.junit.Assert.assertEquals
import org.junit.Test

class RoundProgressLabelTest {

    private val plan = SessionPlan(emptyList(), basisRemainSec = 3600)

    @Test
    fun quiz_countsAllQuestions() {
        val s = SessionState(plan = plan, roundGame = GameKind.REGION_QUIZ, turnInRound = 3, turnsInRound = 6)
        assertEquals("3 / 6 문제", roundProgressLabel(s, humanCount = 2))
    }

    @Test
    fun wordChain_countsTurnsPerPerson() {
        // 2명이 각자 6번 = 사람 차례 12번. 3번 답했으면 모두가 한 바퀴를 돈 것은 1번
        val s = SessionState(plan = plan, roundGame = GameKind.WORD_CHAIN, turnInRound = 3, turnsInRound = 12)
        assertEquals("각자 1 / 6번", roundProgressLabel(s, humanCount = 2))
    }

    @Test
    fun noRound_showsDash() {
        assertEquals("-", roundProgressLabel(SessionState(plan = plan), humanCount = 2))
    }
}
