package px.kinesis.stream.consumer

import akka.NotUsed
import akka.event.LoggingAdapter
import akka.stream.scaladsl.{Keep, Sink, Source}
import akka.stream.{ActorMaterializer, KillSwitch, OverflowStrategy}
import px.kinesis.stream.consumer.checkpoint.CheckpointTracker
import software.amazon.kinesis.processor.{
  ShardRecordProcessor,
  ShardRecordProcessorFactory
}

import scala.collection.immutable.Seq
import scala.concurrent.ExecutionContext

class RecordProcessorFactoryImpl(sink: Sink[Record, NotUsed],
                                 workerId: String,
                                 checkpointTracker: CheckpointTracker,
                                 killSwitch: KillSwitch)(
    implicit am: ActorMaterializer,
    ec: ExecutionContext,
    logging: LoggingAdapter)
    extends ShardRecordProcessorFactory {

  override def shardRecordProcessor(): ShardRecordProcessor = {
    val queue = Source
      .queue[Seq[Record]](0, OverflowStrategy.backpressure)
      .mapConcat(identity)
      .toMat(sink)(Keep.left)
      .run()
    new RecordProcessorImpl(queue, checkpointTracker, killSwitch, workerId)
  }
}
