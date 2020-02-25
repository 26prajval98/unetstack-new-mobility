import org.arl.fjage.*
import static org.arl.unet.Services.*

platform = RealTimePlatform

s_1 = {
    location, time_increment, time, params ->
    f_l = []
    location.each {
        cod ->
        f_l << cod + 1
    }
    return f_l
}

s_2 = {
    location, time_increment, time, params ->
    f_l = []
    f_l[0] = location[0] + 2
    f_l[1] = location[1] + 1
    f_l[2] = location[2]
    return f_l
}

def generateMotionArray(initial_loc, final_loc, time, time_increment){
    distance = 0
    final_loc.eachWithIndex{
        item, index ->
        distance += ((initial_loc[index] - item) * (initial_loc[index] - item))
    }
    
    distance = Math.sqrt(distance)
    speed = distance / time_increment
    
    if(final_loc[0] == initial_loc[0] && final_loc[1] == initial_loc[1] && final_loc[2] == initial_loc[2]){
        heading = 90
        diveRate = 0
    }
    else if(final_loc[0] == initial_loc[0]){
        heading = 90        
        diveRate = (final_loc[2] - initial_loc[2])/time_increment
    }
    else{
        heading = Math.toDegrees(Math.atan((final_loc[0] - initial_loc[0])/(final_loc[1] - initial_loc[1])))
        diveRate = (final_loc[2] - initial_loc[2])/time_increment
    }
    
    return [time: time, speed: speed, heading: heading, diveRate: diveRate]
}

def combine_all_scenarios(location, scenarios, simulation_time = 10.minutes, time_increment = 1.seconds){
    f_l = location
    i_l = location
    motionArray = []
    def time = 0.seconds
    while(time < simulation_time){
        time += time_increment
        scenarios.each{
            scenario ->
                f_l = scenario(f_l, time_increment, time, [])
        }
        motionArray << generateMotionArray(i_l, f_l, time, time_increment)
        i_l = f_l
    }
    return motionArray
}

def m_node(name, location, mobility, scenarios){
    def n = node(name, location: location, mobility: true)
    n.motionModel = combine_all_scenarios(location, scenarios)
}

simulate 480.seconds, {
    def n_1 = m_node('AUV-1', [0.m, 0.m, 0.m], true, [s_2, s_1])
    def n_2 = m_node('AUV-1', [0.m, 0.m, 0.m], true, [s_1])
    def n_3 = m_node('AUV-1', [0.m, 0.m, 0.m], true, [])
}
