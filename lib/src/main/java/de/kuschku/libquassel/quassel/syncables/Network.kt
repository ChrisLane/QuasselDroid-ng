package de.kuschku.libquassel.quassel.syncables

import de.kuschku.libquassel.protocol.*
import de.kuschku.libquassel.protocol.Type
import de.kuschku.libquassel.protocol.primitive.serializer.StringSerializer
import de.kuschku.libquassel.quassel.syncables.interfaces.INetwork
import de.kuschku.libquassel.quassel.syncables.interfaces.INetwork.*
import de.kuschku.libquassel.session.SignalProxy
import de.kuschku.libquassel.util.helpers.getOr
import de.kuschku.libquassel.util.helpers.serializeString
import de.kuschku.libquassel.util.irc.HostmaskHelper
import de.kuschku.libquassel.util.irc.IrcCaseMappers
import io.reactivex.subjects.BehaviorSubject
import java.nio.ByteBuffer
import java.nio.charset.Charset
import java.util.*

class Network constructor(
  networkId: NetworkId,
  proxy: SignalProxy
) : SyncableObject(proxy, "Network"), INetwork {
  override fun init() {
    renameObject("$_networkId")
  }

  override fun fromVariantMap(properties: QVariantMap) {
    initSetCaps(properties["Caps"].valueOr(::emptyMap))
    initSetCapsEnabled(properties["CapsEnabled"].valueOr(::emptyList))
    initSetIrcUsersAndChannels(properties["IrcUsersAndChannels"].valueOr(::emptyMap))
    initSetServerList(properties["ServerList"].valueOr(::emptyList))
    initSetSupports(properties["Supports"].valueOr(::emptyMap))
    initSetProperties(properties)
  }

  override fun toVariantMap(): QVariantMap = mapOf(
    "Caps" to QVariant.of(initCaps(), Type.QVariantMap),
    "CapsEnabled" to QVariant.of(initCapsEnabled(), Type.QVariantList),
    "IrcUsersAndChannels" to QVariant.of(initIrcUsersAndChannels(), Type.QVariantMap),
    "ServerList" to QVariant.of(initServerList(), Type.QVariantList),
    "Supports" to QVariant.of(initSupports(), Type.QVariantMap)
  ) + initProperties()

  fun isMyNick(nick: String) = myNick().equals(nick, true)
  fun isMe(ircUser: IrcUser) = myNick().equals(ircUser.nick(), true)
  fun isChannelName(channelName: String) = when {
    channelName.isBlank() -> false
    supports("CHANTYPES") -> support("CHANTYPES").contains(channelName[0])
    else                  -> "#&!+".contains(channelName[0])
  }

  val caseMapper: IrcCaseMappers.IrcCaseMapper
    get() = IrcCaseMappers[support("CASEMAPPING")]

  /**
   * Checks if the target counts as a STATUSMSG
   *
   * Status messages are prefixed with one or more characters from the server-provided STATUSMSG
   * if available, otherwise "@" and "+" are assumed.  Generally, status messages sent to a
   * channel are only visible to those with the same or higher permissions, e.g. voiced.
   *
   * @param target Name of destination, e.g. a channel or query
   * @return True if a STATUSMSG, otherwise false
   */
  fun isStatusMsg(target: String) = when {
    target.isBlank()      -> false
    supports("STATUSMSG") -> support("STATUSMSG").contains(target[0])
    else                  -> "@+".contains(target[0])
  }

  fun isConnected() = _connected
  fun connectionState() = _connectionState
  fun prefixToMode(prefix: Char): Char? = prefixModes().elementAtOrNull(prefixes().indexOf(prefix))

  fun prefixesToModes(prefixes: String): String = prefixes.mapNotNull {
    prefixes().indexOf(it)
  }.sorted().mapNotNull {
    prefixModes().elementAtOrNull(it)
  }.joinToString("")

  fun modeToPrefix(mode: Char): Char? = prefixes().elementAtOrNull(prefixModes().indexOf(mode))

  fun modesToPrefixes(modes: String): String = modes.mapNotNull {
    prefixModes().indexOf(it)
  }.sorted().mapNotNull {
    prefixes().elementAtOrNull(it)
  }.joinToString("")

  fun channelModeType(mode: Char): ChannelModeType {
    if (_channelModes == null)
      determineChannelModeTypes()
    return _channelModes?.entries
             ?.filter { (_, chars) -> chars.contains(mode) }
             ?.map(Map.Entry<ChannelModeType, Set<Char>>::key)
             ?.firstOrNull() ?: ChannelModeType.NOT_A_CHANMODE
  }

  private fun determineChannelModeTypes() {
    _channelModes = ChannelModeType.validValues
      .zip(
        support("CHANMODES")
          .split(',', limit = ChannelModeType.validValues.size)
          .map(String::toCharArray)
          .map(CharArray::toSet)
      ).toMap()
  }

  fun networkId() = _networkId
  fun networkName() = _networkName
  fun currentServer() = _currentServer
  fun myNick() = _myNick
  fun latency() = _latency
  fun me() = ircUser(myNick())
  fun identity() = _identity
  fun nicks() = _ircUsers.values.map(IrcUser::nick)
  fun channels(): Set<String> = _ircChannels.keys
  fun caps(): Set<String> = _caps.keys
  fun capsEnabled(): Set<String> = _capsEnabled
  fun serverList() = _serverList
  fun useRandomServer() = _useRandomServer
  fun perform() = _perform
  fun useAutoIdentify() = _useAutoIdentify
  fun autoIdentifyService() = _autoIdentifyService
  fun autoIdentifyPassword() = _autoIdentifyPassword
  fun useSasl() = _useSasl
  fun saslAccount() = _saslAccount
  fun saslPassword() = _saslPassword
  fun useAutoReconnect() = _useAutoReconnect
  fun autoReconnectInterval() = _autoReconnectInterval
  fun autoReconnectRetries() = _autoReconnectRetries
  fun unlimitedReconnectRetries() = _unlimitedReconnectRetries
  fun rejoinChannels() = _rejoinChannels
  fun useCustomMessageRate() = _useCustomMessageRate
  fun messageRateBurstSize() = _messageRateBurstSize
  fun messageRateDelay() = _messageRateDelay
  fun unlimitedMessageRate() = _unlimitedMessageRate
  fun networkInfo() = NetworkInfo(
    networkName = networkName(),
    networkId = networkId(),
    identity = identity(),
    codecForServer = codecForServer(),
    codecForEncoding = codecForEncoding(),
    codecForDecoding = codecForDecoding(),
    serverList = serverList(),
    useRandomServer = useRandomServer(),
    perform = perform(),
    useAutoIdentify = useAutoIdentify(),
    autoIdentifyService = autoIdentifyService(),
    autoIdentifyPassword = autoIdentifyPassword(),
    useSasl = useSasl(),
    saslAccount = saslAccount(),
    saslPassword = saslPassword(),
    useAutoReconnect = useAutoReconnect(),
    autoReconnectInterval = autoReconnectInterval(),
    autoReconnectRetries = autoReconnectRetries(),
    unlimitedReconnectRetries = unlimitedReconnectRetries(),
    rejoinChannels = rejoinChannels(),
    useCustomMessageRate = useCustomMessageRate(),
    messageRateBurstSize = messageRateBurstSize(),
    messageRateDelay = messageRateDelay(),
    unlimitedMessageRate = unlimitedMessageRate()
  )

  fun setNetworkInfo(info: NetworkInfo) {
    // we don't set our ID!
    if (!info.networkName.isEmpty() && info.networkName != networkName())
      setNetworkName(info.networkName)
    if (info.identity > 0 && info.identity != identity())
      setIdentity(info.identity)
    if (info.codecForServer != codecForServer())
      setCodecForServer(Charset.forName(info.codecForServer))
    if (info.codecForEncoding != codecForEncoding())
      setCodecForEncoding(Charset.forName(info.codecForEncoding))
    if (info.codecForDecoding != codecForDecoding())
      setCodecForDecoding(Charset.forName(info.codecForDecoding))
    // FIXME compare components
    if (info.serverList.isNotEmpty())
      setServerList(info.serverList.map { QVariant.of(it, QType.Network_Server) })
    if (info.useRandomServer != useRandomServer())
      setUseRandomServer(info.useRandomServer)
    if (info.perform != perform())
      setPerform(info.perform)
    if (info.useAutoIdentify != useAutoIdentify())
      setUseAutoIdentify(info.useAutoIdentify)
    if (info.autoIdentifyService != autoIdentifyService())
      setAutoIdentifyService(info.autoIdentifyService)
    if (info.autoIdentifyPassword != autoIdentifyPassword())
      setAutoIdentifyPassword(info.autoIdentifyPassword)
    if (info.useSasl != useSasl())
      setUseSasl(info.useSasl)
    if (info.saslAccount != saslAccount())
      setSaslAccount(info.saslAccount)
    if (info.saslPassword != saslPassword())
      setSaslPassword(info.saslPassword)
    if (info.useAutoReconnect != useAutoReconnect())
      setUseAutoReconnect(info.useAutoReconnect)
    if (info.autoReconnectInterval != autoReconnectInterval())
      setAutoReconnectInterval(info.autoReconnectInterval)
    if (info.autoReconnectRetries != autoReconnectRetries())
      setAutoReconnectRetries(info.autoReconnectRetries)
    if (info.unlimitedReconnectRetries != unlimitedReconnectRetries())
      setUnlimitedReconnectRetries(info.unlimitedReconnectRetries)
    if (info.rejoinChannels != rejoinChannels())
      setRejoinChannels(info.rejoinChannels)
    // Custom rate limiting
    if (info.useCustomMessageRate != useCustomMessageRate())
      setUseCustomMessageRate(info.useCustomMessageRate)
    if (info.messageRateBurstSize != messageRateBurstSize())
      setMessageRateBurstSize(info.messageRateBurstSize)
    if (info.messageRateDelay != messageRateDelay())
      setMessageRateDelay(info.messageRateDelay)
    if (info.unlimitedMessageRate != unlimitedMessageRate())
      setUnlimitedMessageRate(info.unlimitedMessageRate)
  }

  fun prefixes(): List<Char> {
    if (_prefixes == null)
      determinePrefixes()
    return _prefixes ?: emptyList()
  }

  fun prefixModes(): List<Char> {
    if (_prefixModes == null)
      determinePrefixes()
    return _prefixModes ?: emptyList()
  }

  private fun determinePrefixes() {
    // seems like we have to construct them first
    val prefix = support("PREFIX")
    if (prefix.startsWith("(") && prefix.contains(")")) {
      val (prefixModes, prefixes) = prefix.substring(1)
        .split(')', limit = 2)
        .map(String::toCharArray)
        .map(CharArray::toList)
      _prefixes = prefixes
      _prefixModes = prefixModes
    } else {
      val defaultPrefixes = listOf('~', '&', '@', '%', '+')
      val defaultPrefixModes = listOf('q', 'a', 'o', 'h', 'v')
      if (prefix.isBlank()) {
        _prefixes = defaultPrefixes
        _prefixModes = defaultPrefixModes
        return
      }
      // we just assume that in PREFIX are only prefix chars stored
      val (prefixes, prefixModes) = defaultPrefixes.zip(defaultPrefixModes)
        .filter { prefix.contains(it.second) }
        .unzip()
      _prefixes = prefixes
      _prefixModes = prefixModes
      // check for success
      if (prefixes.isNotEmpty())
        return
      // well... our assumption was obviously wrong...
      // check if it's only prefix modes
      val (prefixes2, prefixModes2) = defaultPrefixes.zip(defaultPrefixModes)
        .filter { prefix.contains(it.first) }
        .unzip()
      _prefixes = prefixes2
      _prefixModes = prefixModes2
      // now we've done all we've could...
    }
  }

  fun channelModes(): Map<ChannelModeType, Set<Char>>? = _channelModes

  fun supports(): Map<String, String> = _supports
  fun supports(param: String) = _supports.contains(param.toUpperCase(Locale.US))
  fun support(param: String) = _supports.getOr(param, "")
  /**
   * Checks if a given capability is advertised by the server.
   *
   * These results aren't valid if the network is disconnected or capability negotiation hasn't
   * happened, and some servers might not correctly advertise capabilities. Don't treat this as a
   * guarantee.
   *
   * @param capability Name of capability
   * @return True if connected and advertised by the server, otherwise false
   */
  fun capAvailable(capability: String) = _caps.contains(capability.toLowerCase(Locale.US))

  /**
   * Checks if a given capability is acknowledged and active.
   *
   * @param capability Name of capability
   * @return True if acknowledged (active), otherwise false
   */
  fun capEnabled(capability: String) = _capsEnabled.contains(capability.toLowerCase(Locale.US))

  /**
   * Gets the value of an available capability, e.g. for SASL, "EXTERNAL,PLAIN".
   *
   * @param capability Name of capability
   * @return Value of capability if one was specified, otherwise isEmpty string
   */
  fun capValue(capability: String) = _caps.getOr(capability.toLowerCase(Locale.US), "")

  /**
   * Check if the given authentication mechanism is likely to be supported.
   *
   * This depends on the server advertising SASL support and either declaring available mechanisms
   * (SASL 3.2), or just indicating something is supported (SASL 3.1).
   *
   * @param saslMechanism  Desired SASL mechanism
   * @return True if mechanism supported or unknown, otherwise false
   */
  fun saslMaybeSupports(saslMechanism: String): Boolean {
    if (!capAvailable(IrcCap.SASL)) {
      // If SASL's not advertised at all, it's likely the mechanism isn't supported, as per specs.
      // Unfortunately, we don't know for sure, but Quassel won't request SASL without it being
      // advertised, anyways.
      // This may also occur if the network's disconnected or negotiation hasn't yet happened.
      return false
    }
    // Get the SASL capability value
    val capValue = capValue(IrcCap.SASL)
    // SASL mechanisms are only specified in capability values as part of SASL 3.2.  In SASL 3.1,
    // it's handled differently.  If we don't know via capability value, assume it's supported to
    // reduce the risk of breaking existing setups.
    // See: http://ircv3.net/specs/extensions/sasl-3.1.html
    // And: http://ircv3.net/specs/extensions/sasl-3.2.html
    return (capValue.isBlank() || capValue.contains(saslMechanism, ignoreCase = true))
  }

  fun newIrcUser(hostMask: String, initData: QVariantMap = emptyMap()): IrcUser {
    val nick = caseMapper.toLowerCase(HostmaskHelper.nick(hostMask))
    val user = ircUser(nick)
    return if (user == null) {
      val ircUser = IrcUser(hostMask, this, proxy)
      ircUser.init()
      if (initData.isNotEmpty()) {
        ircUser.fromVariantMap(initData)
        ircUser.initialized = true
      }
      proxy.synchronize(ircUser)
      _ircUsers[nick] = ircUser
      val mask = ircUser.hostMask()
      super.addIrcUser(mask)
      live_ircUsers.onNext(_ircUsers)
      ircUser
    } else {
      user
    }
  }

  fun ircUser(nickName: String?) = _ircUsers[nickName?.let(caseMapper::toLowerCase)]
  fun liveIrcUser(nickName: String?) = live_ircUsers.map {
    ircUser(
      nickName
    ) ?: IrcUser.NULL
  }.distinctUntilChanged()

  fun ircUsers() = _ircUsers.values.toList()
  fun ircUserCount(): UInt = _ircUsers.size
  fun newIrcChannel(channelName: String, initData: QVariantMap = emptyMap()): IrcChannel =
    ircChannel(channelName).let { channel ->
      return if (channel == null) {
        val ircChannel = IrcChannel(channelName, this, proxy)
        ircChannel.init()
        if (initData.isNotEmpty()) {
          ircChannel.fromVariantMap(initData)
          ircChannel.initialized = true
        }
        proxy.synchronize(ircChannel)
        _ircChannels[caseMapper.toLowerCase(channelName)] = ircChannel
        live_ircChannels.onNext(_ircChannels)
        super.addIrcChannel(channelName)
        ircChannel
      } else {
        channel
      }
    }

  fun ircChannel(channelName: String?) = _ircChannels[channelName?.let(caseMapper::toLowerCase)]
  fun liveIrcChannel(channelName: String?) = live_ircChannels.map {
    ircChannel(
      channelName
    ) ?: IrcChannel.NULL
  }.distinctUntilChanged()

  fun ircChannels() = _ircChannels.values.toList()
  fun ircChannelCount(): UInt = _ircChannels.size
  fun codecForServer(): String = _codecForServer.name()
  fun codecForEncoding(): String = _codecForEncoding.name()
  fun codecForDecoding(): String = _codecForDecoding.name()
  fun setCodecForDecoding(codec: Charset) {
    _codecForDecoding = codec
    super.setCodecForDecoding(Charsets.ISO_8859_1.encode(codecForDecoding()))
  }

  fun setCodecForEncoding(codec: Charset) {
    _codecForEncoding = codec
    super.setCodecForEncoding(Charsets.ISO_8859_1.encode(codecForEncoding()))
  }

  fun setCodecForServer(codec: Charset) {
    _codecForServer = codec
    super.setCodecForServer(Charsets.ISO_8859_1.encode(codecForServer()))
  }

  fun autoAwayActive() = _autoAwayActive
  fun setAutoAwayActive(active: Boolean) {
    _autoAwayActive = active
  }

  override fun setNetworkName(networkName: String) {
    if (_networkName == networkName)
      return
    _networkName = networkName
    super.setNetworkName(networkName)
  }

  override fun setCurrentServer(currentServer: String) {
    if (_currentServer == currentServer)
      return
    _currentServer = currentServer
    super.setCurrentServer(currentServer)
  }

  override fun setConnected(isConnected: Boolean) {
    if (_connected == isConnected)
      return
    _connected = isConnected
    if (!isConnected) {
      setMyNick("")
      setCurrentServer("")
      removeChansAndUsers()
    }
    super.setConnected(isConnected)
  }

  override fun setConnectionState(state: Int) {
    val actualConnectionState = ConnectionState.of(state)
    if (_connectionState == actualConnectionState)
      return
    _connectionState = actualConnectionState
    live_connectionState.onNext(_connectionState)
    super.setConnectionState(state)
  }

  override fun setMyNick(mynick: String) {
    if (_myNick == mynick)
      return
    _myNick = mynick
    if (_myNick.isNotEmpty() && ircUser(myNick()) == null) {
      newIrcUser(myNick())
    }
    super.setMyNick(mynick)
  }

  override fun setLatency(latency: Int) {
    if (_latency == latency)
      return
    _latency = latency
    super.setLatency(latency)
  }

  override fun setIdentity(identity: IdentityId) {
    if (_identity == identity)
      return
    _identity = identity
    super.setIdentity(identity)
  }

  override fun setServerList(serverList: QVariantList) {
    val actualServerList = serverList.map {
      it.valueOrThrow<Server>()
    }
    if (_serverList == actualServerList)
      return
    _serverList = actualServerList
    super.setServerList(serverList)
  }

  override fun setUseRandomServer(randomServer: Boolean) {
    if (_useRandomServer == randomServer)
      return
    _useRandomServer = randomServer
    super.setUseRandomServer(randomServer)
  }

  override fun setPerform(perform: QStringList) {
    val actualPerform = perform.map { it ?: "" }
    if (_perform == actualPerform)
      return
    _perform = actualPerform
    super.setPerform(perform)
  }

  override fun setUseAutoIdentify(autoIdentify: Boolean) {
    if (_useAutoIdentify == autoIdentify)
      return
    _useAutoIdentify = autoIdentify
    super.setUseAutoIdentify(autoIdentify)
  }

  override fun setAutoIdentifyService(service: String) {
    if (_autoIdentifyService == service)
      return
    _autoIdentifyService = service
    super.setAutoIdentifyService(service)
  }

  override fun setAutoIdentifyPassword(password: String) {
    if (_autoIdentifyPassword == password)
      return
    _autoIdentifyPassword = password
    super.setAutoIdentifyPassword(password)
  }

  override fun setUseSasl(sasl: Boolean) {
    if (_useSasl == sasl)
      return
    _useSasl = sasl
    super.setUseSasl(sasl)
  }

  override fun setSaslAccount(account: String) {
    if (_saslAccount == account)
      return
    _saslAccount = account
    super.setSaslAccount(account)
  }

  override fun setSaslPassword(password: String) {
    if (_saslPassword == password)
      return
    _saslPassword = password
    super.setSaslPassword(password)
  }

  override fun setUseAutoReconnect(autoReconnect: Boolean) {
    if (_useAutoReconnect == autoReconnect)
      return
    _useAutoReconnect = autoReconnect
    super.setUseAutoReconnect(autoReconnect)
  }

  override fun setAutoReconnectInterval(interval: UInt) {
    if (_autoReconnectInterval == interval)
      return
    _autoReconnectInterval = interval
    super.setAutoReconnectInterval(interval)
  }

  override fun setAutoReconnectRetries(retries: UShort) {
    if (_autoReconnectRetries == retries)
      return
    _autoReconnectRetries = retries
    super.setAutoReconnectRetries(retries)
  }

  override fun setUnlimitedReconnectRetries(unlimitedRetries: Boolean) {
    if (_unlimitedReconnectRetries == unlimitedRetries)
      return
    _unlimitedReconnectRetries = unlimitedRetries
    super.setUnlimitedReconnectRetries(unlimitedRetries)
  }

  override fun setRejoinChannels(rejoinChannels: Boolean) {
    if (_rejoinChannels == rejoinChannels)
      return
    _rejoinChannels = rejoinChannels
    super.setRejoinChannels(rejoinChannels)
  }

  /**
   * Sets whether or not custom rate limiting is used.
   *
   * Setting limits too low may value you disconnected from the server!
   *
   * @param useCustomRate If true, use custom rate limits, otherwise use Quassel defaults.
   */

  override fun setUseCustomMessageRate(useCustomRate: Boolean) {
    if (_useCustomMessageRate == useCustomRate)
      return
    _useCustomMessageRate = useCustomRate
    super.setUseCustomMessageRate(useCustomRate)
  }

  override fun setMessageRateBurstSize(burstSize: UInt) {
    if (_messageRateBurstSize == burstSize)
      return
    if (burstSize < 1)
      throw IllegalArgumentException("Message Burst Size must be a positive number: $burstSize")
    _messageRateBurstSize = burstSize
    super.setMessageRateBurstSize(burstSize)
  }

  override fun setMessageRateDelay(messageDelay: UInt) {
    if (_messageRateDelay == messageDelay)
      return
    if (messageDelay < 1)
      throw IllegalArgumentException("Message Delay must be a positive number: $messageDelay")
    _messageRateDelay = messageDelay
    super.setMessageRateDelay(messageDelay)
  }

  override fun setUnlimitedMessageRate(unlimitedRate: Boolean) {
    if (_unlimitedMessageRate == unlimitedRate)
      return
    _unlimitedMessageRate = unlimitedRate
    super.setUnlimitedMessageRate(unlimitedRate)
  }

  override fun setCodecForDecoding(codecName: ByteBuffer?) {
    if (codecName != null)
      setCodecForDecoding(Charsets.ISO_8859_1.decode(codecName).toString())
  }

  override fun setCodecForEncoding(codecName: ByteBuffer?) {
    if (codecName != null)
      setCodecForEncoding(Charsets.ISO_8859_1.decode(codecName).toString())
  }

  override fun setCodecForServer(codecName: ByteBuffer?) {
    if (codecName != null)
      setCodecForServer(Charsets.ISO_8859_1.decode(codecName).toString())
  }

  fun setCodecForDecoding(codecName: String) {
    setCodecForDecoding(Charset.forName(codecName))
  }

  fun setCodecForEncoding(codecName: String) {
    setCodecForDecoding(Charset.forName(codecName))
  }

  fun setCodecForServer(codecName: String) {
    setCodecForDecoding(Charset.forName(codecName))
  }

  override fun addSupport(param: String, value: String) {
    _supports[param] = value
    super.addSupport(param, value)
  }

  override fun removeSupport(param: String) {
    if (!_supports.contains(param))
      return
    _supports.remove(param)
    super.removeSupport(param)
  }

  override fun addCap(capability: String, value: String) {
    _caps[capability.toLowerCase(Locale.US)] = value
    super.addCap(capability, value)
  }

  override fun acknowledgeCap(capability: String) {
    val lowerCase = capability.toLowerCase(Locale.US)
    if (!_capsEnabled.contains(lowerCase))
      return
    _capsEnabled.add(lowerCase)
    super.acknowledgeCap(capability)
  }

  override fun removeCap(capability: String) {
    val lowerCase = capability.toLowerCase(Locale.US)
    if (!_caps.contains(lowerCase))
      return
    _caps.remove(lowerCase)
    _capsEnabled.remove(lowerCase)
    super.removeCap(capability)
  }

  override fun clearCaps() {
    if (_caps.isEmpty() && _capsEnabled.isEmpty())
      return
    _caps.clear()
    _capsEnabled.clear()
    super.clearCaps()
  }

  override fun addIrcUser(hostmask: String) {
    newIrcUser(hostmask)
  }

  override fun addIrcChannel(channel: String) {
    newIrcChannel(channel)
  }

  override fun initSupports(): QVariantMap = _supports.entries.map { (key, value) ->
    key to QVariant.of(value, Type.QString)
  }.toMap()

  override fun initCaps(): QVariantMap = _caps.entries.map { (key, value) ->
    key to QVariant.of(value, Type.QString)
  }.toMap()

  override fun initCapsEnabled(): QVariantList = _capsEnabled.map {
    QVariant.of(it, Type.QString)
  }.toList()

  override fun initServerList(): QVariantList = _serverList.map {
    QVariant.of(it, QType.Network_Server)
  }.toList()

  override fun initIrcUsersAndChannels(): QVariantMap {
    return mapOf(
      "Users" to QVariant.of(_ircUsers.values.map { it.toVariantMap() }.transpose().map {
        QVariant.of(it, Type.QVariantList)
      }, Type.QVariantMap
      ),
      "Channels" to QVariant.of(_ircChannels.values.map { it.toVariantMap() }.transpose().map {
        QVariant.of(it, Type.QVariantList)
      }, Type.QVariantMap
      )
    )
  }

  override fun initProperties(): QVariantMap = mapOf(
    "networkName" to QVariant.of(networkName(), Type.QString),
    "currentServer" to QVariant.of(currentServer(), Type.QString),
    "myNick" to QVariant.of(myNick(), Type.QString),
    "latency" to QVariant.of(latency(), Type.Int),
    "codecForServer" to QVariant.of(codecForServer().serializeString(StringSerializer.UTF8),
                                    Type.QByteArray
    ),
    "codecForEncoding" to QVariant.of(codecForEncoding().serializeString(StringSerializer.UTF8),
                                      Type.QByteArray
    ),
    "codecForDecoding" to QVariant.of(codecForDecoding().serializeString(StringSerializer.UTF8),
                                      Type.QByteArray
    ),
    "identityId" to QVariant.of(identity(), QType.IdentityId),
    "isConnected" to QVariant.of(isConnected(), Type.Bool),
    "connectionState" to QVariant.of(connectionState(), Type.Int),
    "useRandomServer" to QVariant.of(useRandomServer(), Type.Bool),
    "perform" to QVariant.of(perform(), Type.QStringList),
    "useAutoIdentify" to QVariant.of(useAutoIdentify(), Type.Bool),
    "autoIdentifyService" to QVariant.of(autoIdentifyService(), Type.QString),
    "autoIdentifyPassword" to QVariant.of(autoIdentifyPassword(), Type.QString),
    "useSasl" to QVariant.of(useSasl(), Type.Bool),
    "saslAccount" to QVariant.of(saslAccount(), Type.QString),
    "saslPassword" to QVariant.of(saslPassword(), Type.QString),
    "useAutoReconnect" to QVariant.of(useAutoReconnect(), Type.Bool),
    "autoReconnectInterval" to QVariant.of(autoReconnectInterval(), Type.UInt),
    "autoReconnectRetries" to QVariant.of(autoReconnectRetries(), Type.UShort),
    "unlimitedReconnectRetries" to QVariant.of(unlimitedReconnectRetries(), Type.Bool),
    "rejoinChannels" to QVariant.of(rejoinChannels(), Type.Bool),
    "useCustomMessageRate" to QVariant.of(useCustomMessageRate(), Type.Bool),
    "msgRateBurstSize" to QVariant.of(messageRateBurstSize(), Type.UInt),
    "msgRateMessageDelay" to QVariant.of(messageRateDelay(), Type.UInt),
    "unlimitedMessageRate" to QVariant.of(unlimitedMessageRate(), Type.Bool)
  )

  override fun initSetSupports(supports: QVariantMap) {
    supports.entries.map { (key, value) -> key to value.value("") }.toMap(_supports)
  }

  override fun initSetCaps(caps: QVariantMap) {
    caps.entries.map { (key, value) -> key to value.value("") }.toMap(_supports)
  }

  override fun initSetCapsEnabled(capsEnabled: QVariantList) {
    capsEnabled.mapNotNull { it.value<String?>() }.toCollection(_capsEnabled)
  }

  override fun initSetServerList(serverList: QVariantList) {
    _serverList = serverList.mapNotNull { it.value<QVariantMap?>() }.map(
      Server.Companion::fromVariantMap
    ).toMutableList()
  }

  override fun initSetIrcUsersAndChannels(usersAndChannels: QVariantMap) {
    if (initialized)
      throw IllegalArgumentException("Received init data for network ${networkId()} after init")
    val users: Map<String, QVariant_> = usersAndChannels["Users"].valueOr(::emptyMap)
    val userKeys = users.keys
    users["nick"].valueOr<QVariantList>(::emptyList).forEachIndexed { index, nick ->
      val data = mutableMapOf<String, QVariant_>()
      for (it in userKeys) {
        val value = users[it].value<QVariantList>()?.get(index)
        if (value != null)
          data[it] = value
      }
      newIrcUser(nick.value(""), data)
    }

    val channels: Map<String, QVariant_> = usersAndChannels["Channels"].valueOr(::emptyMap)
    val channelKeys = channels.keys
    channels["name"].valueOr<QVariantList>(::emptyList).forEachIndexed { index, nick ->
      val data = mutableMapOf<String, QVariant_>()
      for (it in channelKeys) {
        val value = channels[it].value<QVariantList>()?.get(index)
        if (value != null)
          data[it] = value
      }
      newIrcChannel(nick.value(""), data)
    }
  }

  override fun initSetProperties(properties: QVariantMap) {
    setNetworkName(properties["networkName"].valueOr(this::networkName))
    setCurrentServer(properties["currentServer"].valueOr(this::currentServer))
    setMyNick(properties["myNick"].valueOr(this::myNick))
    setLatency(properties["latency"].valueOr(this::latency))
    setCodecForServer(
      properties["codecForServer"].value(codecForServer().serializeString(StringSerializer.UTF8))
    )
    setCodecForEncoding(
      properties["codecForEncoding"].value(
        codecForEncoding().serializeString(StringSerializer.UTF8)
      )
    )
    setCodecForDecoding(
      properties["codecForDecoding"].value(
        codecForDecoding().serializeString(StringSerializer.UTF8)
      )
    )
    setIdentity(properties["identityId"].valueOr(this::identity))
    setConnected(properties["isConnected"].valueOr(this::isConnected))
    setConnectionState(properties["connectionState"].value(connectionState().value))
    setUseRandomServer(properties["useRandomServer"].valueOr(this::useRandomServer))
    setPerform(properties["perform"].valueOr(this::perform))
    setUseAutoIdentify(properties["useAutoIdentify"].valueOr(this::useAutoIdentify))
    setAutoIdentifyService(properties["autoIdentifyService"].valueOr(this::autoIdentifyService))
    setAutoIdentifyPassword(properties["autoIdentifyPassword"].valueOr(this::autoIdentifyPassword))
    setUseSasl(properties["useSasl"].valueOr(this::useSasl))
    setSaslAccount(properties["saslAccount"].valueOr(this::saslAccount))
    setSaslPassword(properties["saslPassword"].valueOr(this::saslPassword))
    setUseAutoReconnect(properties["useAutoReconnect"].valueOr(this::useAutoReconnect))
    setAutoReconnectInterval(
      properties["autoReconnectInterval"].valueOr(this::autoReconnectInterval)
    )
    setAutoReconnectRetries(properties["autoReconnectRetries"].valueOr(this::autoReconnectRetries))
    setUnlimitedReconnectRetries(
      properties["unlimitedReconnectRetries"].valueOr(this::unlimitedReconnectRetries)
    )
    setRejoinChannels(properties["rejoinChannels"].valueOr(this::rejoinChannels))
    setUseCustomMessageRate(properties["useCustomMessageRate"].valueOr(this::useCustomMessageRate))
    setMessageRateBurstSize(properties["msgRateBurstSize"].valueOr(this::messageRateBurstSize))
    setMessageRateDelay(properties["msgRateMessageDelay"].valueOr(this::messageRateDelay))
    setUnlimitedMessageRate(properties["unlimitedMessageRate"].valueOr(this::unlimitedMessageRate))
  }

  fun updateNickFromMask(mask: String): IrcUser {
    val nick = caseMapper.toLowerCase(HostmaskHelper.nick(mask))
    val user = _ircUsers[nick]
    return if (user != null) {
      user.updateHostmask(mask)
      user
    } else {
      newIrcUser(mask)
    }
  }

  override fun ircUserNickChanged(old: String, new: String) {
    val value = _ircUsers.remove(caseMapper.toLowerCase(old))
    if (value != null) {
      _ircUsers[caseMapper.toLowerCase(new)] = value
    }
  }

  override fun emitConnectionError(error: String) {
  }

  fun removeChansAndUsers() {
    _ircUsers.clear()
    _ircChannels.clear()
    live_ircChannels.onNext(_ircChannels)
    live_ircUsers.onNext(_ircUsers)
  }

  fun removeIrcUser(user: IrcUser) {
    _ircUsers.remove(caseMapper.toLowerCase(user.nick()))
    live_ircUsers.onNext(_ircUsers)
  }

  fun removeIrcChannel(channel: IrcChannel) {
    _ircChannels.remove(caseMapper.toLowerCase(channel.name()))
    live_ircChannels.onNext(_ircChannels)
  }

  private var _networkId: NetworkId = networkId
  private var _identity: IdentityId = -1
  private var _myNick: String = ""
  private var _latency: Int = 0
  private var _networkName: String = "<not initialized>"
  private var _currentServer: String = ""
  private var _connected: Boolean = false
  private var _connectionState: ConnectionState = ConnectionState.Disconnected
  val live_connectionState = BehaviorSubject.createDefault(ConnectionState.Disconnected)
  private var _prefixes: List<Char>? = null
  private var _prefixModes: List<Char>? = null
  private var _channelModes: Map<ChannelModeType, Set<Char>>? = null
  // stores all known nicks for the server
  private var _ircUsers: MutableMap<String, IrcUser> = mutableMapOf()
  private val live_ircUsers = BehaviorSubject.createDefault(emptyMap<String, IrcUser>())
  // stores all known channels
  private var _ircChannels: MutableMap<String, IrcChannel> = mutableMapOf()
  private val live_ircChannels = BehaviorSubject.createDefault(emptyMap<String, IrcChannel>())
  // stores results from RPL_ISUPPORT
  private var _supports: MutableMap<String, String> = mutableMapOf()
  /**
   * Capabilities supported by the IRC server
   * By synchronizing the supported capabilities, the client could suggest certain behaviors, e.g.
   * in the Network settings dialog, recommending SASL instead of using NickServ, or warning if
   * SASL EXTERNAL isn't available.
   */
  private var _caps: MutableMap<String, String> = mutableMapOf()
  /**
   * Enabled capabilities that received 'CAP ACK'
   * _capsEnabled uses the same values from the <name>=<value> pairs stored in _caps
   */
  private var _capsEnabled: MutableSet<String> = mutableSetOf()
  private var _serverList: List<Server> = mutableListOf()
  private var _useRandomServer: Boolean = false
  private var _perform: List<String> = mutableListOf()
  private var _useAutoIdentify: Boolean = false
  private var _autoIdentifyService: String = ""
  private var _autoIdentifyPassword: String = ""
  private var _useSasl: Boolean = false
  private var _saslAccount: String = ""
  private var _saslPassword: String = ""
  private var _useAutoReconnect: Boolean = false
  private var _autoReconnectInterval: UInt = 60
  private var _autoReconnectRetries: UShort = 10
  private var _unlimitedReconnectRetries = false
  private var _rejoinChannels = false
  // Custom rate limiting
  /** If true, use custom rate limits, otherwise use defaults */
  private var _useCustomMessageRate: Boolean = false
  /** Maximum number of messages to send without any delays */
  private var _messageRateBurstSize: UInt = 5
  /** Delay in ms. for messages when max. burst messages sent */
  private var _messageRateDelay: UInt = 2200
  /** If true, disable rate limiting, otherwise apply limits */
  private var _unlimitedMessageRate: Boolean = false
  private var _codecForServer: Charset = Charsets.UTF_8
  private var _codecForEncoding: Charset = Charsets.UTF_8
  private var _codecForDecoding: Charset = Charsets.UTF_8
  /** when this is active handle305 and handle306 don't trigger any output */
  private var _autoAwayActive: Boolean = false

  companion object {
    val NULL = Network(-1, SignalProxy.NULL)
  }
}