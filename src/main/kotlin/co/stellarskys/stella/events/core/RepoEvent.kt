package co.stellarskys.stella.events.core

import co.stellarskys.stella.api.events.Event
import tech.thatgravyboat.repolib.api.RepoStatus

object RepoEvent {
    class Success: Event()
    class Sataus(val status: RepoStatus): Event()
}