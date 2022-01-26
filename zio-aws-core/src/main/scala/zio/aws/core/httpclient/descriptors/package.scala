package zio.aws.core.httpclient

import zio.config.ConfigDescriptor._
import zio.config._

import java.net.{NetworkInterface, SocketOption, StandardSocketOptions}
import scala.util.Try

package object descriptors {

  def socketOption[T, JT](
      opt: SocketOption[JT],
      fromJava: JT => T,
      toJava: T => JT
  )(desc: ConfigDescriptor[T]): ConfigDescriptor[Option[OptionValue[JT]]] =
    nested(opt.name())(desc).optional.transform(
      _.map(value => OptionValue(opt, toJava(value))),
      opt => opt.map(_.value).map(fromJava)
    )

  def boolSocketOption(
      opt: SocketOption[java.lang.Boolean]
  ): ConfigDescriptor[Option[OptionValue[java.lang.Boolean]]] =
    socketOption[Boolean, java.lang.Boolean](
      opt,
      (b: java.lang.Boolean) => b.booleanValue(),
      (b: Boolean) => java.lang.Boolean.valueOf(b)
    )(boolean)

  def intSocketOption(
      opt: SocketOption[java.lang.Integer]
  ): ConfigDescriptor[Option[OptionValue[java.lang.Integer]]] =
    socketOption[Int, java.lang.Integer](
      opt,
      (i: java.lang.Integer) => i.intValue(),
      (i: Int) => java.lang.Integer.valueOf(i)
    )(int)

  val networkInterfaceByName: ConfigDescriptor[NetworkInterface] =
    string.transformOrFail(
      name =>
        Try(NetworkInterface.getByName(name)).toEither.left.map(_.getMessage),
      iface => Right(iface.getName)
    )

  val channelOptions: ConfigDescriptor[ChannelOptions] = {
    import StandardSocketOptions._

    def findOpt[T](
        options: ChannelOptions,
        key: SocketOption[_]
    ): Option[OptionValue[T]] =
      options.options
        .find { opt =>
          opt.key == key
        }
        .asInstanceOf[Option[OptionValue[T]]]

    (boolSocketOption(
      SO_BROADCAST
    ) ?? "Allow transmission of broadcast datagrams" zip
      boolSocketOption(SO_KEEPALIVE) ?? "Keep connection alive" zip
      intSocketOption(SO_SNDBUF) ?? "The size of the socket send buffer" zip
      intSocketOption(
        SO_RCVBUF
      ) ?? "The size of the socket receive buffer" zip
      boolSocketOption(SO_REUSEADDR) ?? "Re-use address" zip
      intSocketOption(SO_LINGER) ?? "Linger on close if data is present" zip
      intSocketOption(IP_TOS) ?? "The ToS octet in the IP header" zip
      socketOption(
        IP_MULTICAST_IF,
        identity[NetworkInterface],
        identity[NetworkInterface]
      )(
        networkInterfaceByName
      ) ?? "The network interface's name for IP multicast datagrams" zip
      intSocketOption(
        IP_MULTICAST_TTL
      ) ?? "The time-to-live for IP multicast datagrams" zip
      boolSocketOption(
        IP_MULTICAST_LOOP
      ) ?? "Loopback for IP multicast datagrams" zip
      boolSocketOption(TCP_NODELAY) ?? "Disable the Nagle algorithm")
      .transform(
        tuple =>
          ChannelOptions(tuple.productIterator.collect {
            case Some(opt: OptionValue[_]) =>
              opt.asInstanceOf[OptionValue[Any]]
          }.toVector),
        channelOptions =>
          (
            findOpt(channelOptions, SO_BROADCAST),
            findOpt(channelOptions, SO_KEEPALIVE),
            findOpt(channelOptions, SO_SNDBUF),
            findOpt(channelOptions, SO_RCVBUF),
            findOpt(channelOptions, SO_REUSEADDR),
            findOpt(channelOptions, SO_LINGER),
            findOpt(channelOptions, IP_TOS),
            findOpt(channelOptions, IP_MULTICAST_IF),
            findOpt(channelOptions, IP_MULTICAST_TTL),
            findOpt(channelOptions, IP_MULTICAST_LOOP),
            findOpt(channelOptions, TCP_NODELAY)
          )
      )
  }
}
