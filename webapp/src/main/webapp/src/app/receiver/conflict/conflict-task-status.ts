export class ConflictTaskStatus {

	running: boolean = false;

	lastUpdated?: Date;

	constructor(running: boolean, lastUpdated: Date) {
		this.running = running;
		this.lastUpdated = lastUpdated;
	}

}
